package com.pethotel.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Счёт клиента за одно бронирование. Один bookingId → один Invoice.
// Создаётся по booking.created, финализируется по booking.completed,
// пополняется diningAmount по каждому order.created с extraCharge > 0.
// Инвариант: totalAmount == roomAmount + amenitiesAmount + diningAmount.
@Entity
@Table(name = "invoices", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // bookingId / customerId — "shared nothing": не FK к другим сервисам, только Long
    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long customerId;

    // Три составляющих счёта — обновляются независимо через разные Kafka-события
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal roomAmount;       // стоимость ночей (из booking.completed)

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amenitiesAmount; // дополнительные услуги (из booking.completed)

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal diningAmount;    // накопительный счёт буфета (из order.created)

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;     // пересчитывается при каждом обновлении

    @Enumerated(EnumType.STRING)  // хранить "UNPAID"/"PAID", а не 0/1 — читаемо в БД
    @Column(nullable = false)
    private InvoiceStatus status;

    private LocalDateTime createdAt;

    @PrePersist  // JPA-хук: устанавливается один раз перед первым INSERT
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
