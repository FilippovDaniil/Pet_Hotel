package com.pethotel.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Точка входа API Gateway (порт 8080).
// Единственная точка входа для всех HTTP-запросов фронтенда.
// Ответственности: JWT-валидация (JwtAuthFilter) + маршрутизация к downstream-сервисам.
// Маршруты настраиваются в application.yml через spring.cloud.gateway.routes.
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
