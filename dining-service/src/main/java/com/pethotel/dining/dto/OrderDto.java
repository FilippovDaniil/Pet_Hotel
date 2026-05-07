package com.pethotel.dining.dto;

import com.pethotel.dining.entity.DeliveryType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// DTO ответа заказа. Включает финансовую разбивку:
//   totalAmount  — полная стоимость (price × quantity)
//   paidByLimit  — покрыто лимитом буфета (0 для ORDINARY-гостей)
//   extraCharge  — сумма к оплате клиентом (попадает в итоговый счёт через billing-service)
@Data
public class OrderDto {
    private Long id;
    private Long bookingId;
    private Long customerId;
    private Long menuItemId;
    private String menuItemName;   // snapshot названия на момент заказа
    private Integer quantity;
    private BigDecimal totalAmount;
    private LocalDateTime orderTime;
    private BigDecimal paidByLimit;
    private BigDecimal extraCharge;
    private DeliveryType deliveryType;
}
