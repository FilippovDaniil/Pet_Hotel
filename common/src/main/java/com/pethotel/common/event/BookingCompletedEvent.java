package com.pethotel.common.event;

import com.pethotel.common.enums.RoomClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCompletedEvent {
    private Long bookingId;
    private Long customerId;
    private Long roomId;
    private RoomClass roomClass;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private BigDecimal roomTotal;
    private BigDecimal amenitiesTotal;
}
