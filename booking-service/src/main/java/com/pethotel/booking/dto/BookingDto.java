package com.pethotel.booking.dto;

import com.pethotel.common.enums.BookingStatus;
import com.pethotel.common.enums.RoomClass;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// DTO ответа с полной информацией о бронировании.
// Включает вложенный список AmenityDto — фронтенд показывает услуги в карточке бронирования.
@Data
public class BookingDto {
    private Long id;
    private Long customerId;
    private Long roomId;
    private RoomClass roomClass;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BookingStatus status;      // PENDING / CONFIRMED / CANCELLED / COMPLETED
    private BigDecimal totalPrice;     // итог: проживание + все услуги
    private LocalDateTime createdAt;
    private List<AmenityDto> amenities; // список дополнительных услуг с ценами
}
