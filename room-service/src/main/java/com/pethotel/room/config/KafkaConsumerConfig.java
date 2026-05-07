package com.pethotel.room.config;

import com.pethotel.common.event.BookingCancelledEvent;
import com.pethotel.common.event.BookingCreatedEvent;
import com.pethotel.common.kafka.KafkaTopics;
import com.pethotel.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Kafka-потребитель для room-service.
// Название класса "KafkaConsumerConfig" немного вводит в заблуждение — это не @Configuration,
// а @Component-компонент, который просто содержит методы-обработчики Kafka-сообщений.
// Инфраструктура Kafka (десериализация, группы потребителей) настроена в application.yml.
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final RoomService roomService;

    // @KafkaListener — Spring Kafka регистрирует этот метод как обработчик топика.
    //   topics = KafkaTopics.BOOKING_CREATED ("booking.created") — константа из common, не строка.
    //   groupId = "room-service" — Consumer Group ID: Kafka запоминает, до какого offset'а
    //     читал этот сервис. Если сервис перезапустится — продолжит с места остановки.
    //
    // Jackson десериализует JSON-сообщение из топика в BookingCreatedEvent.
    // spring.json.trusted.packages в application.yml разрешает десериализацию классов из common.
    // spring.json.add.type.headers: false (на стороне producer) — сообщения не содержат тип-заголовок,
    //   поэтому тип определяется сигнатурой метода (BookingCreatedEvent), а не заголовком.
    @KafkaListener(topics = KafkaTopics.BOOKING_CREATED, groupId = "room-service")
    public void onBookingCreated(BookingCreatedEvent event) {
        log.info("Kafka received booking.created: bookingId={}", event.getBookingId());
        // Блокируем даты номера: создаём строки в room_unavailable_dates.
        roomService.blockDates(event);
    }

    // При отмене бронирования — освобождаем даты, чтобы номер снова стал доступен для поиска.
    @KafkaListener(topics = KafkaTopics.BOOKING_CANCELLED, groupId = "room-service")
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("Kafka received booking.cancelled: bookingId={}", event.getBookingId());
        roomService.unblockDates(event);
    }
}
