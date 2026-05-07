package com.pethotel.booking.dto;

import com.pethotel.common.enums.ServiceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// DTO одной дополнительной услуги в ответе.
// В отличие от AmenityBookingRequest, здесь есть id и price —
// клиент может видеть, сколько стоила каждая услуга с учётом его привилегий.
@Data
public class AmenityDto {
    private Long id;
    private ServiceType serviceType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal price;  // итоговая цена: 0 для PREMIUM-привилегий
}
