package com.pethotel.room.dto;

import com.pethotel.common.enums.RoomClass;
import lombok.Data;

import java.math.BigDecimal;

// DTO для представления номера в API-ответах.
// Не включает список unavailableDates — клиенту не нужна внутренняя структура блокировки дат,
// только бизнес-поля: класс, вместимость, цена.
@Data
public class RoomDto {
    private Long id;
    private String roomNumber;
    private RoomClass roomClass;
    private Integer capacity;
    private BigDecimal pricePerNight;
    private String description;
}
