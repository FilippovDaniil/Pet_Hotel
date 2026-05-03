package com.pethotel.amenity.config;

import com.pethotel.common.event.ServiceBookedEvent;
import com.pethotel.common.kafka.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumerConfig {

    @KafkaListener(topics = KafkaTopics.SERVICE_BOOKED, groupId = "amenity-service")
    public void onServiceBooked(ServiceBookedEvent event) {
        log.info("Kafka received service.booked: bookingId={} serviceType={} price={}",
                event.getBookingId(), event.getServiceType(), event.getPrice());
    }
}
