package com.pethotel.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Kafka-событие: бронирование отменено.
//
// Producer:  booking-service (при переходе PENDING/CONFIRMED → CANCELLED)
// Consumers:
//   - room-service → разблокирует даты номера (roomId + период хранится в room-service)
//
// penaltyAmount — штраф за позднюю отмену (30% от totalPrice если заезд в течение 24 ч).
// Равен BigDecimal.ZERO если отмена без штрафа или выполнена сотрудником ресепшн.
// Поле передаётся для информации; billing-service на этот топик не подписан —
// штраф учитывается прямо в booking-service при создании итогового счёта.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelledEvent {
    private Long bookingId;          // какое бронирование отменено
    private Long customerId;         // кому принадлежало
    private Long roomId;             // какой номер освобождается
    private BigDecimal penaltyAmount; // штраф (0 если отмена без санкций)
}
