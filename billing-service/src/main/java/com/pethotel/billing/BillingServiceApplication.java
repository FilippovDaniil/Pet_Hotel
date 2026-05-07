package com.pethotel.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Точка входа billing-service (порт 8086, схема БД: billing).
// Отвечает за: создание счетов, накопление расходов через Kafka, оплату.
// Не имеет DataSeeder — счета создаются только через Kafka-события от других сервисов.
@SpringBootApplication
public class BillingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
