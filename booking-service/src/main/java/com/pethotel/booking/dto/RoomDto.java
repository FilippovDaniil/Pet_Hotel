package com.pethotel.booking.dto;

import com.pethotel.common.enums.RoomClass;
import lombok.Data;

import java.math.BigDecimal;

// Локальная копия RoomDto для ответа от room-service через WebClient.
// Booking-service не зависит от room-service как JAR — только через HTTP.
// Для десериализации JSON нужен локальный класс с теми же полями.
// Содержит только поля, которые booking-service реально использует:
//   roomClass → для AmenityPriceCalculator
//   pricePerNight → для расчёта стоимости проживания (nights × pricePerNight)
@Data
public class RoomDto {
    private Long id;
    private String roomNumber;
    private RoomClass roomClass;
    private Integer capacity;
    private BigDecimal pricePerNight;
}
