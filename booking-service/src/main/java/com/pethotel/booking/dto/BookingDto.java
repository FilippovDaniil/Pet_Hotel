package com.pethotel.booking.dto;

import com.pethotel.common.enums.BookingStatus;
import com.pethotel.common.enums.RoomClass;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingDto {
    private Long id;
    private Long customerId;
    private Long roomId;
    private RoomClass roomClass;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BookingStatus status;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private List<AmenityDto> amenities;
}
