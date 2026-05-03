package com.pethotel.billing.config;

import com.pethotel.billing.service.BillingService;
import com.pethotel.common.event.BookingCompletedEvent;
import com.pethotel.common.event.OrderCreatedEvent;
import com.pethotel.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final BillingService billingService;

    @KafkaListener(topics = KafkaTopics.BOOKING_COMPLETED, groupId = "billing-service")
    public void onBookingCompleted(BookingCompletedEvent event) {
        log.info("Kafka received booking.completed: bookingId={} customerId={}",
                event.getBookingId(), event.getCustomerId());
        billingService.createInvoice(event);
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "billing-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Kafka received order.created: orderId={} bookingId={} extraCharge={}",
                event.getOrderId(), event.getBookingId(), event.getExtraCharge());
        if (event.getExtraCharge() != null && event.getExtraCharge().signum() > 0) {
            billingService.addDiningCharge(event.getBookingId(), event.getExtraCharge());
        }
    }
}
