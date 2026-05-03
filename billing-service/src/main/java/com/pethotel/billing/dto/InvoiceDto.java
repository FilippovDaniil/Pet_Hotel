package com.pethotel.billing.dto;

import com.pethotel.billing.entity.InvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InvoiceDto {
    private Long id;
    private Long bookingId;
    private Long customerId;
    private BigDecimal roomAmount;
    private BigDecimal amenitiesAmount;
    private BigDecimal diningAmount;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private LocalDateTime createdAt;
}
