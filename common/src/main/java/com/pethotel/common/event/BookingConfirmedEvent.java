package com.pethotel.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Kafka-событие: бронирование подтверждено ресепшн.
//
// Producer:  booking-service (при переходе PENDING → CONFIRMED)
// Consumers: нет активных потребителей на текущем этапе проекта.
//            Топик зарезервирован для будущей отправки email-уведомлений клиенту.
//
// Минимальный состав: достаточно двух полей, чтобы любой потребитель
// мог сопоставить событие с нужным бронированием и клиентом.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent {
    private Long bookingId;   // какое бронирование подтверждено
    private Long customerId;  // кому принадлежит
}
