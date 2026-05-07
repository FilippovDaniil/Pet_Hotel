package com.pethotel.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

// Утилита для работы с JWT (JSON Web Token) — stateless-токенами аутентификации.
//
// Структура JWT: <header>.<payload>.<signature>
//   header  — алгоритм подписи (HS256)
//   payload — claims: subject (email), userId, role, iat (issued at), exp (expiration)
//   signature — HMAC-SHA256 подпись, созданная из header+payload+secret
//
// Класс намеренно не является @Component — чтобы не зависеть от Spring в common-библиотеке.
// Каждый сервис, которому нужен JWT, регистрирует JwtUtil как @Bean через AppConfig
// с параметрами из своего application.yml (jwt.secret, jwt.expiration-ms).
// Это позволяет переиспользовать логику без Spring-зависимости в common.
public class JwtUtil {

    // SecretKey — бинарное представление секрета, готовое для HMAC.
    // Держим объект, а не строку: Keys.hmacShaKeyFor() уже выполнил кодирование,
    // поэтому при каждом вызове не надо заново конвертировать.
    private final SecretKey key;

    // Время жизни токена в миллисекундах (обычно 86400000 = 24 часа).
    private final long expirationMs;

    // Конструктор вызывается из AppConfig каждого сервиса через @Bean.
    // secret берётся из env JWT_SECRET (мин. 32 символа для HS256).
    public JwtUtil(String secret, long expirationMs) {
        // hmacShaKeyFor — фабричный метод JJWT; преобразует байты секрета в javax.crypto.SecretKey.
        // StandardCharsets.UTF_8 — фиксируем кодировку, чтобы результат был одинаков на любой JVM.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // Генерирует подписанный JWT-токен.
    // Вызывается только в customer-service (при регистрации и логине).
    public String generateToken(Long userId, String email, String role) {
        return Jwts.builder()
                // subject — стандартный claim JWT; здесь храним email (он же "кто этот токен").
                .subject(email)
                // Дополнительные custom claims: userId (Long) и role (String).
                // Map.of() — неизменяемая карта; порядок в JSON не гарантирован, но это не важно.
                .claims(Map.of("userId", userId, "role", role))
                // issuedAt — момент выпуска токена (стандартный claim "iat").
                .issuedAt(new Date())
                // expiration — момент истечения (стандартный claim "exp").
                // System.currentTimeMillis() + expirationMs → абсолютная метка времени в UTC.
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                // signWith — создаёт HMAC-SHA256 подпись; алгоритм выбирается автоматически по длине ключа.
                .signWith(key)
                // compact() — финальная сборка: кодирует header+payload в Base64URL, вычисляет подпись,
                // склеивает через точки → строка вида "eyJ...".
                .compact();
    }

    // Парсит и верифицирует токен; бросает исключение если токен повреждён, истёк или подпись неверна.
    // Используется внутри класса (в isValid, getUserId, getRole, getEmail).
    public Claims parseToken(String token) {
        return Jwts.parser()
                // verifyWith — задаём ключ для проверки подписи.
                .verifyWith(key)
                .build()
                // parseSignedClaims — парсит все три части, проверяет подпись и срок действия.
                // Бросает ExpiredJwtException, MalformedJwtException, SecurityException и др.
                .parseSignedClaims(token)
                // getPayload() — возвращает декодированный объект Claims (Map-like).
                .getPayload();
    }

    // Возвращает true если токен валиден; перехватывает любые исключения из parseToken.
    // Используется в JwtAuthFilter (api-gateway) для быстрой проверки перед извлечением данных.
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Извлекает userId из custom claim.
    // get("userId", Long.class) — типизированное извлечение; Jackson внутри JJWT десериализует число как Long.
    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    // Извлекает роль пользователя (строка "CUSTOMER", "RECEPTION" или "ADMIN").
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    // Извлекает email из стандартного claim "subject".
    // getSubject() — синоним get("sub", String.class).
    public String getEmail(String token) {
        return parseToken(token).getSubject();
    }
}
