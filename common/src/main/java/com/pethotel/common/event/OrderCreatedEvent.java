package com.pethotel.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Kafka-событие: клиент сделал заказ в буфете.
//
// Producer:  dining-service (при каждом новом заказе еды)
// Consumers:
//   - billing-service → добавляет extraCharge к открытому счёту бронирования
//
// Логика разделения стоимости (DailyLimitService в dining-service):
//   totalAmount  = полная стоимость заказа
//   paidByLimit  = часть, покрытая дневным лимитом номера (MIDDLE: 1000 руб, PREMIUM: 3000 руб)
//   extraCharge  = totalAmount - paidByLimit → именно эта сумма попадёт в счёт
//
// Пример: MIDDLE, заказ на 1500 руб при использованном лимите 800 руб.
//   paidByLimit = 200 руб (оставшийся лимит), extraCharge = 1300 руб.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;              // ID заказа в dining-service
    private Long bookingId;            // к какому бронированию привязан заказ
    private Long customerId;
    private BigDecimal totalAmount;    // полная стоимость заказа
    private BigDecimal paidByLimit;    // покрыто лимитом (не добавляется к счёту)
    private BigDecimal extraCharge;    // превышение лимита → в Invoice
    private LocalDateTime orderTime;   // момент заказа (для истории)
}
