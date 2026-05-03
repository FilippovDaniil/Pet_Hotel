package com.pethotel.booking.dto;

import com.pethotel.common.enums.ServiceType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AmenityBookingRequest {
    @NotNull
    private ServiceType serviceType;
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
}
