package com.pethotel.room.dto;

import com.pethotel.common.enums.RoomClass;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

// DTO запроса для создания и обновления номера (POST/PUT /api/rooms).
// Одинаковый для create и update — упрощение: все поля обязательны при обоих операциях.
@Data
public class RoomRequest {

    // @NotBlank — номер комнаты не может быть пустой строкой или null.
    @NotBlank
    private String roomNumber;

    // @NotNull — roomClass не может быть null; Jackson вернёт ошибку валидации если поле отсутствует.
    @NotNull
    private RoomClass roomClass;

    // @Min(1) — вместимость минимум 1 гость; 0 или отрицательное значение недопустимо.
    @NotNull @Min(1)
    private Integer capacity;

    // @DecimalMin("0.01") — цена должна быть положительной; BigDecimal сравнивается как строка.
    //   Нельзя создать номер с нулевой или отрицательной ценой.
    @NotNull @DecimalMin("0.01")
    private BigDecimal pricePerNight;

    // Описание необязательно — нет @NotNull/@NotBlank.
    private String description;
}
