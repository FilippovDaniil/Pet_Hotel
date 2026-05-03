package com.pethotel.common.event;

import com.pethotel.common.enums.ServiceType;
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
public class ServiceBookedEvent {
    private Long bookingId;
    private Long customerId;
    private ServiceType serviceType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal price;
}
