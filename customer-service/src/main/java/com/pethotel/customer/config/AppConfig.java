package com.pethotel.customer.config;

import com.pethotel.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Фабричный конфиг: регистрирует бины, которые нельзя пометить @Component
// (например, классы из сторонних библиотек или из common-модуля, не зависящего от Spring).
@Configuration
public class AppConfig {

    // @Value — инжектирует значение из application.yml (или env-переменной JWT_SECRET).
    // Если переменная не задана и нет default — Spring не поднимется (fail-fast).
    @Value("${jwt.secret}")
    private String jwtSecret;

    // :86400000 — значение по умолчанию (24 часа в мс), если jwt.expiration-ms не задан.
    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    // @Bean — результат метода регистрируется в Spring-контексте.
    // JwtUtil не является @Component, поэтому без этого бина Spring не смог бы его инжектировать
    // в CustomerService. Такой подход сохраняет common-модуль независимым от Spring.
    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(jwtSecret, jwtExpirationMs);
    }
}
