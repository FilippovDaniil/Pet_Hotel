package com.pethotel.common.event;

import com.pethotel.common.enums.RoomClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Kafka-событие: бронирование завершено (гость выехал).
//
// Producer:  booking-service (при переходе CONFIRMED → COMPLETED через checkOut)
// Consumers:
//   - billing-service → финализирует счёт: фиксирует roomTotal и amenitiesTotal,
//                       добавляет пункты в Invoice и отправляет его клиенту
//
// Два финансовых поля разделены намеренно:
//   roomTotal      — стоимость проживания (nights × pricePerNight)
//   amenitiesTotal — стоимость всех дополнительных услуг (SAUNA, MASSAGE и т.д.)
// billing-service суммирует их вместе с заказами из буфета (OrderCreatedEvent).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCompletedEvent {
    private Long bookingId;
    private Long customerId;
    private Long roomId;
    private RoomClass roomClass;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private BigDecimal roomTotal;       // итог за проживание
    private BigDecimal amenitiesTotal;  // итог за дополнительные услуги
}
