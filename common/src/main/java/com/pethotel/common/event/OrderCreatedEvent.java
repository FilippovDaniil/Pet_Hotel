package com.pethotel.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private Long bookingId;
    private Long customerId;
    private BigDecimal totalAmount;
    private BigDecimal paidByLimit;
    private BigDecimal extraCharge;
    private LocalDateTime orderTime;
}
