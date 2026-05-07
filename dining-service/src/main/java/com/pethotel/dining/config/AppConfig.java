package com.pethotel.dining.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// Регистрирует WebClient.Builder для межсервисного HTTP-вызова:
// dining-service → booking-service (получить класс номера для расчёта лимита буфета).
@Configuration
public class AppConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
