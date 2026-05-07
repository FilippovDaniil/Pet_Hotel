package com.pethotel.gateway.config;

import com.pethotel.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Регистрирует JwtUtil как Spring Bean для использования в JwtAuthFilter.
// JwtUtil из common не помечен @Component — требует явного @Bean здесь.
// secret и expirationMs должны совпадать с customer-service (один и тот же JWT_SECRET).
@Configuration
public class AppConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(jwtSecret, jwtExpirationMs);  // HMAC-SHA ключ создаётся один раз при старте
    }
}
