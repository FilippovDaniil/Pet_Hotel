package com.pethotel.billing.config;

import com.pethotel.billing.service.BillingService;
import com.pethotel.common.event.BookingCompletedEvent;
import com.pethotel.common.event.BookingCreatedEvent;
import com.pethotel.common.event.OrderCreatedEvent;
import com.pethotel.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Kafka-потребители billing-service. Три топика формируют полный жизненный цикл счёта:
//   booking.created   → черновик счёта (предварительная сумма)
//   booking.completed → финализация (точные room + amenities суммы)
//   order.created     → накопление сверхлимитных расходов буфета (только если extraCharge > 0)
// groupId = "billing-service" — каждый экземпляр сервиса получает своё подмножество партиций.
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final BillingService billingService;

    // booking.created: создать черновик Invoice с предварительной суммой
    @KafkaListener(topics = KafkaTopics.BOOKING_CREATED, groupId = "billing-service")
    public void onBookingCreated(BookingCreatedEvent event) {
        log.info("Kafka received booking.created: bookingId={} customerId={}",
                event.getBookingId(), event.getCustomerId());
        billingService.initInvoice(event);
    }

    // booking.completed: обновить счёт точными суммами room + amenities после выезда
    @KafkaListener(topics = KafkaTopics.BOOKING_COMPLETED, groupId = "billing-service")
    public void onBookingCompleted(BookingCompletedEvent event) {
        log.info("Kafka received booking.completed: bookingId={} customerId={}",
                event.getBookingId(), event.getCustomerId());
        billingService.createInvoice(event);
    }

    // order.created: добавить сверхлимитный расход буфета к счёту.
    // signum() > 0: игнорируем заказы, полностью покрытые лимитом (extraCharge == 0).
    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "billing-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Kafka received order.created: orderId={} bookingId={} extraCharge={}",
                event.getOrderId(), event.getBookingId(), event.getExtraCharge());
        if (event.getExtraCharge() != null && event.getExtraCharge().signum() > 0) {
            billingService.addDiningCharge(event.getBookingId(), event.getExtraCharge());
        }
    }
}
