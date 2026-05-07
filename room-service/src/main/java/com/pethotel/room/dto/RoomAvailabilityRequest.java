package com.pethotel.room.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

// DTO для запроса поиска свободных номеров.
// Параметры передаются через query string: GET /api/rooms/search?checkIn=2025-06-01&checkOut=2025-06-05&guests=2
// Spring автоматически конвертирует строки в LocalDate через встроенный конвертер.
@Data
public class RoomAvailabilityRequest {

    @NotNull
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    // guests = 1 — значение по умолчанию: если параметр не передан, ищем номера минимум на 1 гостя.
    // @Min(1) — нельзя искать номера "на 0 гостей".
    @Min(1)
    private int guests = 1;
}
