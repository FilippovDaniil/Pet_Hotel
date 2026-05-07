package com.pethotel.billing.dto;

import com.pethotel.billing.entity.InvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Полное представление счёта для фронтенда.
// Показывает разбивку по составляющим, чтобы клиент понимал, за что платит.
@Data
public class InvoiceDto {
    private Long id;
    private Long bookingId;
    private Long customerId;
    private BigDecimal roomAmount;       // стоимость проживания
    private BigDecimal amenitiesAmount; // доп. услуги (сауна, массаж...)
    private BigDecimal diningAmount;    // сверхлимитные расходы буфета
    private BigDecimal totalAmount;     // итог = сумма трёх составляющих
    private InvoiceStatus status;       // UNPAID / PAID
    private LocalDateTime createdAt;
}
