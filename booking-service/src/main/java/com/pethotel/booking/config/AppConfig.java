package com.pethotel.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// Регистрирует WebClient.Builder как @Bean.
// Spring Boot auto-configuration не создаёт WebClient.Builder автоматически в servlet-окружении —
// его нужно объявить явно, чтобы RoomClient мог получить его через @RequiredArgsConstructor.
@Configuration
public class AppConfig {

    // WebClient.Builder (а не WebClient) — рекомендуется Spring: Builder можно переиспользовать
    // для создания нескольких клиентов с разными baseUrl, таймаутами и interceptors.
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
