package com.pethotel.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// DTO запроса на создание бронирования (POST /api/bookings).
// Клиент может сразу указать дополнительные услуги вместе с бронированием.
@Data
public class BookingRequest {

    @NotNull
    private Long roomId;

    @NotNull
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    // @Valid на списке — запускает валидацию каждого AmenityBookingRequest внутри списка.
    // Без @Valid аннотации @NotNull внутри AmenityBookingRequest будут проигнорированы.
    // Список не обязателен (нет @NotNull) — клиент может бронировать без услуг.
    @Valid
    private List<AmenityBookingRequest> amenities = new ArrayList<>();
}
