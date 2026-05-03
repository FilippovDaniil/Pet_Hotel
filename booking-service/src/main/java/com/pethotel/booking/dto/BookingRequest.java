package com.pethotel.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class BookingRequest {
    @NotNull
    private Long roomId;
    @NotNull
    private LocalDate checkIn;
    @NotNull
    private LocalDate checkOut;
    @Valid
    private List<AmenityBookingRequest> amenities = new ArrayList<>();
}
