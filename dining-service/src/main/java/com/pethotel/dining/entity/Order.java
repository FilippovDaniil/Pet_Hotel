package com.pethotel.dining.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Заказ из буфета. Хранит как финансовую разбивку (totalAmount = paidByLimit + extraCharge),
// так и snapshot имени позиции на момент заказа.
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

    // bookingId — в рамках какого бронирования сделан заказ (используется для привязки к лимиту).
    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long menuItemId;

    // menuItemName — денормализованное название: защита от ситуации "переименовали блюдо — история изменилась".
    // columnDefinition = "varchar(255) default 'Неизвестно'" — DDL-уровень default для старых записей.
    @Column(nullable = false, columnDefinition = "varchar(255) default 'Неизвестно'")
    private String menuItemName;

    @Column(nullable = false)
    private Integer quantity;

    // totalAmount = price × quantity — полная стоимость заказа.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Заполняется через @PrePersist — автоматически, не из запроса.
    private LocalDateTime orderTime;

    // paidByLimit  — часть стоимости, покрытая дневным лимитом (не попадает в счёт клиента).
    // extraCharge  — превышение лимита → отправляется в billing-service через Kafka (order.created).
    // Инвариант: paidByLimit + extraCharge == totalAmount.
    @Column(precision = 10, scale = 2)
    private BigDecimal paidByLimit;

    @Column(precision = 10, scale = 2)
    private BigDecimal extraCharge;

    // columnDefinition задаёт SQL default: если клиент не передал deliveryType — ставим ROOM_DELIVERY.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(50) default 'ROOM_DELIVERY'")
    private DeliveryType deliveryType;

    // @PrePersist — JPA-хук: вызывается Hibernate перед INSERT.
    // Гарантирует, что orderTime всегда заполнен в момент сохранения.
    @PrePersist
    public void prePersist() {
        this.orderTime = LocalDateTime.now();
    }
}
