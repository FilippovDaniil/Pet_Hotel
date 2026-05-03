package com.pethotel.dining.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {

    @NotNull(message = "Booking ID must not be null")
    private Long bookingId;

    @NotNull(message = "Menu item ID must not be null")
    private Long menuItemId;

    @NotNull(message = "Quantity must not be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
