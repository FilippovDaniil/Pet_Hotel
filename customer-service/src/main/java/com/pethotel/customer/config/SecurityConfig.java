package com.pethotel.customer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// @Configuration — класс содержит @Bean-методы; обрабатывается Spring при старте.
// @EnableWebSecurity — активирует Spring Security; без неё класс игнорируется.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // SecurityFilterChain — цепочка фильтров, через которую проходит каждый HTTP-запрос.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF-защита предназначена для браузерных форм с сессией.
            // Мы используем JWT (stateless) → CSRF неактуален, отключаем.
            .csrf(csrf -> csrf.disable())

            // STATELESS — Spring не создаёт HTTP-сессию.
            // Каждый запрос обрабатывается независимо; контекст безопасности не сохраняется между ними.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Разрешаем все запросы — аутентификация выполнена на уровне API Gateway.
            // /api/auth/** — публичные пути (регистрация, логин), не требуют JWT.
            // Остальные пути Gateway пропускает только с валидным токеном.
            // Дублировать проверку JWT здесь не нужно — это downstream-сервис.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }

    // BCryptPasswordEncoder — реализация PasswordEncoder, используемая в CustomerService.
    // Регистрируем как @Bean, чтобы Spring инжектировал его через конструктор (DI).
    // BCrypt автоматически генерирует соль при каждом encode() и включает её в хэш,
    // поэтому одинаковые пароли дают разные хэши — защита от rainbow-таблиц.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
