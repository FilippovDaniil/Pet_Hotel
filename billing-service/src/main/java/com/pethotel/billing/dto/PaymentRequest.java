package com.pethotel.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

// Тело запроса на оплату счёта (POST /{bookingId}/pay).
// amount — сумма к списанию; должна совпадать с invoice.totalAmount на фронтенде.
// Валидация защищает от нулевых или отрицательных значений на уровне Spring MVC.
@Data
public class PaymentRequest {

    @NotNull(message = "Booking ID must not be null")
    private Long bookingId;

    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")  // исключает 0 и отрицательные
    private BigDecimal amount;
}
