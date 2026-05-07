package com.pethotel.amenity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Точка входа amenity-service. Порт 8082 (задан в application.yml).
// Ответственность: хранение справочника дополнительных услуг отеля (сауна, баня, бассейн и т.д.)
// с поддержкой загрузки изображений (bytea в PostgreSQL) и Kafka-consumer для учёта заказов.
@SpringBootApplication
public class AmenityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AmenityServiceApplication.class, args);
    }
}
