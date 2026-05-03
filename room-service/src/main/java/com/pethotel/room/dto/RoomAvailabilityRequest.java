package com.pethotel.room.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RoomAvailabilityRequest {
    @NotNull
    private LocalDate checkIn;
    @NotNull
    private LocalDate checkOut;
    @Min(1)
    private int guests = 1;
}
