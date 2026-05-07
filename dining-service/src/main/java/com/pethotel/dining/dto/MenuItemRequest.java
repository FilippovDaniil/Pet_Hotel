package com.pethotel.dining.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

// DTO для создания и обновления позиции меню (POST/PUT /api/menu).
@Data
public class MenuItemRequest {

    @NotBlank(message = "Name must not be blank")
    private String name;

    @NotNull(message = "Price must not be null")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    private BigDecimal price;

    @NotBlank(message = "Category must not be blank")
    private String category;

    // available = true по умолчанию — новая позиция сразу видна в меню.
    private boolean available = true;
}
