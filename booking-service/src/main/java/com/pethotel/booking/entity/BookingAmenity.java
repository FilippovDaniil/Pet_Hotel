package com.pethotel.booking.entity;

import com.pethotel.common.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Одна дополнительная услуга в рамках бронирования.
// Каждый тип услуги и временной слот — отдельная строка.
// Клиент может забронировать несколько услуг в одном запросе (BookingRequest.amenities).
@Entity
@Table(name = "booking_amenities", schema = "booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingAmenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne LAZY — ссылка на родительское бронирование.
    // LAZY: объект Booking подгружается только при явном обращении к полю.
    // При загрузке Booking.amenities Hibernate сам подтягивает все BookingAmenity через JOIN или N+1.
    // @JoinColumn — эта сторона связи владеет FK-колонкой booking_id.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    // Конкретный временной слот: startTime/endTime используются для проверки пересечений
    // (BookingAmenityRepository.findConflicting) и для AmenityService.
    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    // Итоговая цена с учётом привилегий класса номера (рассчитана AmenityPriceCalculator).
    // Может быть BigDecimal.ZERO для PREMIUM-гостей на первую SAUNA/BATH/MASSAGE.
    @Column(nullable = false)
    private BigDecimal price;
}
