package com.pethotel.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

// Unit-тест для JwtUtil. Не требует Spring-контекста — JwtUtil не является @Component.
// Тест покрывает happy-path (корректный токен) и все негативные сценарии (истёкший, подделанный, мусор).
class JwtUtilTest {

    // Тестовый секрет ≥ 32 символов — минимум для HMAC-SHA256 (иначе WeakKeyException).
    private static final String SECRET = "test-secret-key-min-32-chars-here-ok";
    // 1 час в миллисекундах — достаточно для прохождения тестов, не влияет на скорость.
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtUtil jwtUtil;

    // @BeforeEach — JUnit 5: этот метод выполняется перед каждым тестом заново.
    // Создаём свежий экземпляр, чтобы тесты не влияли друг на друга.
    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
    }

    // Проверяем, что generateToken возвращает непустую строку (базовая проверка работоспособности).
    @Test
    void generateToken_returnsNonBlankToken() {
        assertThat(jwtUtil.generateToken(1L, "user@example.com", "CUSTOMER")).isNotBlank();
    }

    // Round-trip: записали userId → прочитали userId. Проверяет сохранность числового claim.
    @Test
    void getUserId_roundTrip() {
        String token = jwtUtil.generateToken(42L, "user@example.com", "CUSTOMER");
        assertThat(jwtUtil.getUserId(token)).isEqualTo(42L);
    }

    // Round-trip для email: хранится в стандартном claim "subject".
    @Test
    void getEmail_roundTrip() {
        String token = jwtUtil.generateToken(1L, "test@hotel.com", "ADMIN");
        assertThat(jwtUtil.getEmail(token)).isEqualTo("test@hotel.com");
    }

    // Round-trip для роли: хранится в custom claim "role".
    @Test
    void getRole_roundTrip() {
        String token = jwtUtil.generateToken(1L, "user@example.com", "RECEPTION");
        assertThat(jwtUtil.getRole(token)).isEqualTo("RECEPTION");
    }

    // Нормальный токен должен проходить валидацию.
    @Test
    void isValid_trueForValidToken() {
        String token = jwtUtil.generateToken(1L, "user@example.com", "CUSTOMER");
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    // Создаём JwtUtil с TTL = 1 мс → ждём 20 мс → токен должен быть просрочен.
    // InterruptedException объявлен в сигнатуре — JUnit 5 сам его обработает если прервут тред.
    @Test
    void isValid_falseForExpiredToken() throws InterruptedException {
        JwtUtil shortLived = new JwtUtil(SECRET, 1L); // TTL = 1 мс
        String token = shortLived.generateToken(1L, "user@example.com", "CUSTOMER");
        Thread.sleep(20); // ждём просрочки
        assertThat(shortLived.isValid(token)).isFalse();
    }

    // Имитируем атаку: заменяем последние 6 символов (часть подписи) на "XXXXXX".
    // JJWT должен обнаружить несовпадение подписи и вернуть false.
    @Test
    void isValid_falseForTamperedSignature() {
        String token = jwtUtil.generateToken(1L, "user@example.com", "CUSTOMER");
        String tampered = token.substring(0, token.length() - 6) + "XXXXXX"; // портим подпись
        assertThat(jwtUtil.isValid(tampered)).isFalse();
    }

    // Токен, подписанный другим секретом, не должен проходить проверку этим jwtUtil.
    // Имитирует попытку использовать токен из другого окружения.
    @Test
    void isValid_falseForDifferentSecret() {
        JwtUtil other = new JwtUtil("other-secret-key-min-32-chars-here-ok", EXPIRATION_MS);
        String token = other.generateToken(1L, "user@example.com", "CUSTOMER");
        assertThat(jwtUtil.isValid(token)).isFalse(); // подпись не совпадёт с нашим секретом
    }

    // Полностью невалидная строка (не JWT) → isValid должен вернуть false, а не бросить исключение.
    @Test
    void isValid_falseForGarbage() {
        assertThat(jwtUtil.isValid("not.a.jwt")).isFalse();
    }

    // Проверяем parseToken напрямую: все три claim должны сохраниться и извлечься корректно.
    @Test
    void parseToken_containsAllClaims() {
        String token = jwtUtil.generateToken(7L, "admin@hotel.com", "ADMIN");
        Claims claims = jwtUtil.parseToken(token); // бросит исключение если токен невалиден
        assertThat(claims.getSubject()).isEqualTo("admin@hotel.com"); // claim "sub"
        assertThat(claims.get("userId", Long.class)).isEqualTo(7L);   // custom claim
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN"); // custom claim
    }
}
