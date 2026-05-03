package com.pethotel.dining.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MenuItemDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private String category;
    private boolean available;
}
