package com.pethotel.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Точка входа booking-service. Сервис поднимается на порту 8083.
// Зависимости: PostgreSQL (схема "booking"), Kafka (producer), WebClient → room-service.
// Самый "тяжёлый" сервис: содержит бизнес-логику жизненного цикла бронирования,
// расчёт привилегий, межсервисное HTTP-взаимодействие и публикацию 4 разных Kafka-событий.
@SpringBootApplication
public class BookingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
