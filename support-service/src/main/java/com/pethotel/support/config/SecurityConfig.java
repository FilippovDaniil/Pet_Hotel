package com.pethotel.support.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// @Configuration — класс содержит @Bean-методы; Spring обрабатывает его при старте.
// @EnableWebSecurity — активирует механизм Spring Security (без неё класс игнорируется).
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // @Bean — результат метода регистрируется в Spring-контексте как компонент.
    // SecurityFilterChain — цепочка фильтров, через которую проходит каждый HTTP-запрос.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF (Cross-Site Request Forgery) — защита для браузерных форм с сессией.
            // Мы используем JWT (stateless) — CSRF неактуален, отключаем.
            .csrf(csrf -> csrf.disable())

            // STATELESS — Spring Security не создаёт HTTP-сессию и не хранит SecurityContext между запросами.
            // Каждый запрос аутентифицируется независимо (через заголовки от API Gateway).
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // permitAll() — разрешаем все запросы без проверки.
            // Авторизация уже выполнена на уровне API Gateway (JwtAuthFilter).
            // Downstream-сервисы доверяют заголовкам X-User-Id и X-User-Role — проверять JWT повторно не нужно.
            // Это упрощение: в production-среде стоило бы дополнительно проверять заголовки.
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
