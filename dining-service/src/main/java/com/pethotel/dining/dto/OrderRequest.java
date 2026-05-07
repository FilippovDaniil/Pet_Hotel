package com.pethotel.dining.dto;

import com.pethotel.dining.entity.DeliveryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// DTO запроса на создание заказа (POST /api/orders).
// Цена не передаётся клиентом — рассчитывается из menuItem.price × quantity на сервере.
@Data
public class OrderRequest {

    @NotNull(message = "Booking ID must not be null")
    private Long bookingId;      // к какому бронированию привязать заказ (для лимита и истории)

    @NotNull(message = "Menu item ID must not be null")
    private Long menuItemId;

    @NotNull(message = "Quantity must not be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Delivery type must not be null")
    private DeliveryType deliveryType;  // ROOM_DELIVERY или DINING_ROOM
}
