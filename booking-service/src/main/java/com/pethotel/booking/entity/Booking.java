package com.pethotel.booking.entity;

import com.pethotel.common.enums.BookingStatus;
import com.pethotel.common.enums.RoomClass;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Главная сущность сервиса: одно бронирование клиента на конкретный номер.
@Entity
@Table(name = "bookings", schema = "booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // customerId и roomId — "внешние ключи" без JPA-связи (@ManyToOne).
    // Намеренно храним только ID: booking-service не владеет данными клиентов и номеров.
    // Это микросервисный паттерн "shared nothing" — каждый сервис управляет только своими данными.
    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long roomId;

    // roomClass денормализован: скопирован из room-service при создании бронирования.
    // Нужен, чтобы не делать запрос к room-service каждый раз при расчёте привилегий.
    @Enumerated(EnumType.STRING)
    private RoomClass roomClass;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    // totalPrice = стоимость проживания + стоимость всех выбранных услуг (amenities).
    // Обновляется в BookingService.create() после расчёта цен услуг.
    @Column(nullable = false)
    private BigDecimal totalPrice;

    // createdAt заполняется автоматически — не передаётся из запроса.
    private LocalDateTime createdAt;

    // @OneToMany + cascade ALL + orphanRemoval — то же, что в Room.unavailableDates.
    // mappedBy = "booking" — владеет связью BookingAmenity (там @JoinColumn).
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingAmenity> amenities = new ArrayList<>();

    // @PrePersist — JPA-хук: метод вызывается Hibernate автоматически ПЕРЕД первым INSERT.
    // Гарантирует, что createdAt всегда заполнен, независимо от того, забыл ли вызывающий код его установить.
    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
