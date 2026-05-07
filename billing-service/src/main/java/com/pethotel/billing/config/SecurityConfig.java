package com.pethotel.billing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// Стандартная stateless-конфигурация downstream-сервиса.
// Аутентификация выполнена на API Gateway; billing-service доверяет X-User-Id из заголовка.
// Авторизация по роли (RECEPTION для оплаты) — ответственность фронтенда и gateway.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                                               // REST API, CSRF не нужен
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // без HttpSession
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());              // gateway уже проверил JWT
        return http.build();
    }
}
