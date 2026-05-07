package com.pethotel.room;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication включает @Configuration + @EnableAutoConfiguration + @ComponentScan
// по пакету com.pethotel.room и всем вложенным пакетам.
// Сервис поднимается на порту 8082 (задан в application.yml).
// Дополнительно активен Redis-кэш (@EnableCaching в CacheConfig) и Kafka-consumer (KafkaConsumerConfig).
@SpringBootApplication
public class RoomServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoomServiceApplication.class, args);
    }
}
