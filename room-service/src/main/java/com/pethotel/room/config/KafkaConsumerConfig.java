package com.pethotel.room.config;

import com.pethotel.common.event.BookingCancelledEvent;
import com.pethotel.common.event.BookingCreatedEvent;
import com.pethotel.common.kafka.KafkaTopics;
import com.pethotel.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final RoomService roomService;

    @KafkaListener(topics = KafkaTopics.BOOKING_CREATED, groupId = "room-service")
    public void onBookingCreated(BookingCreatedEvent event) {
        log.info("Kafka received booking.created: bookingId={}", event.getBookingId());
        roomService.blockDates(event);
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CANCELLED, groupId = "room-service")
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("Kafka received booking.cancelled: bookingId={}", event.getBookingId());
        roomService.unblockDates(event);
    }
}
