package com.pethotel.common.event;

import com.pethotel.common.enums.RoomClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Kafka-событие: бронирование создано.
//
// Producer:  booking-service (при успешном сохранении нового бронирования)
// Consumers:
//   - room-service    → блокирует даты номера на период checkIn–checkOut
//   - billing-service → создаёт черновик счёта с начальной суммой totalPrice
//
// @Data          — Lombok: геттеры, сеттеры, equals, hashCode, toString (нужны для Jackson).
// @Builder       — удобный builder-паттерн при создании события в booking-service.
// @NoArgsConstructor — требуется Jackson для десериализации: он создаёт объект пустым конструктором,
//                      затем проставляет поля через сеттеры.
// @AllArgsConstructor — нужен для совместной работы с @Builder.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreatedEvent {
    private Long bookingId;        // ID бронирования в booking-service
    private Long customerId;       // ID клиента для связи со счётом
    private Long roomId;           // ID номера → room-service заблокирует его на эти даты
    private RoomClass roomClass;   // класс номера → billing-service сохраняет для будущего счёта
    private LocalDate checkIn;     // дата заезда (inclusive)
    private LocalDate checkOut;    // дата выезда (exclusive)
    private BigDecimal totalPrice; // предварительная стоимость (только проживание, без услуг и еды)
}
