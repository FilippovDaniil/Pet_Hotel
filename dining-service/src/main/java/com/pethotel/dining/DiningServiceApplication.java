package com.pethotel.dining;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Точка входа dining-service. Порт 8085.
// Ответственность: меню буфета (CRUD), приём заказов, расчёт дневного лимита по классу номера.
// Зависимости: PostgreSQL (схема "dining"), Redis (дневные лимиты), Kafka (publisher), WebClient → booking-service.
@SpringBootApplication
public class DiningServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiningServiceApplication.class, args);
    }
}
