package com.pethotel.amenity.dto;

import com.pethotel.common.enums.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AmenityRequest {

    @NotBlank(message = "Name must not be blank")
    private String name;

    @NotNull(message = "Type must not be null")
    private ServiceType type;

    @NotNull(message = "Default price must not be null")
    @DecimalMin(value = "0.01", message = "Default price must be at least 0.01")
    private BigDecimal defaultPrice;

    @Min(value = 1, message = "Max duration must be at least 1 minute")
    private int maxDurationMinutes;
}
