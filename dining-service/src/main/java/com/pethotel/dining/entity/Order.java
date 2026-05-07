package com.pethotel.dining.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders", schema = "dining")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long menuItemId;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'Неизвестно'")
    private String menuItemName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    private LocalDateTime orderTime;

    @Column(precision = 10, scale = 2)
    private BigDecimal paidByLimit;

    @Column(precision = 10, scale = 2)
    private BigDecimal extraCharge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(50) default 'ROOM_DELIVERY'")
    private DeliveryType deliveryType;

    @PrePersist
    public void prePersist() {
        this.orderTime = LocalDateTime.now();
    }
}
