package com.pethotel.dining.dto;

import lombok.Data;

import java.math.BigDecimal;

// DTO позиции меню для публичного API. Не включает внутренние детали (id в БД используется
// только для ссылки при создании заказа, поэтому включён — клиент передаёт menuItemId в OrderRequest).
@Data
public class MenuItemDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private String category;
    private boolean available;
}
