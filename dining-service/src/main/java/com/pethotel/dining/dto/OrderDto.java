package com.pethotel.dining.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDto {
    private Long id;
    private Long bookingId;
    private Long customerId;
    private Long menuItemId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private LocalDateTime orderTime;
    private BigDecimal paidByLimit;
    private BigDecimal extraCharge;
}
