package com.pethotel.room.dto;

import com.pethotel.common.enums.RoomClass;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomRequest {
    @NotBlank
    private String roomNumber;
    @NotNull
    private RoomClass roomClass;
    @NotNull @Min(1)
    private Integer capacity;
    @NotNull @DecimalMin("0.01")
    private BigDecimal pricePerNight;
    private String description;
}
