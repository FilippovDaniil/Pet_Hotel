package com.pethotel.dining.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// Позиция меню буфета. Простая справочная сущность без связей с другими таблицами.
// При заказе имя позиции денормализуется в Order.menuItemName (защита от изменения меню после заказа).
@Entity
@Table(name = "menu_items", schema = "dining")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // NUMERIC(10, 2) — точный денежный тип PostgreSQL; аналог BigDecimal в Java.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Категория: "Завтрак", "Обед", "Ужин", "Напитки", "Десерты" — строковая, не enum.
    // Гибче: категории можно добавлять без изменения кода.
    @Column(nullable = false)
    private String category;

    // available = false — позиция скрыта из меню (временно недоступна, не удалена).
    // Хранить историю заказов даже после удаления позиции позволяет именно этот флаг.
    @Column(nullable = false)
    private boolean available;
}
