package com.pethotel.booking.dto;

import com.pethotel.common.enums.ServiceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AmenityDto {
    private Long id;
    private ServiceType serviceType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal price;
}
