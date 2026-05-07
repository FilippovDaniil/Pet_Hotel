package com.pethotel.customer.entity;

import com.pethotel.common.enums.Role;
import jakarta.persistence.*;
import lombok.*;

// @Entity — JPA-маркер: Hibernate отображает этот класс на таблицу в БД.
// @Table   — указываем имя таблицы и схему; иначе Hibernate взял бы имя класса ("customer").
//            schema = "customer" соответствует PostgreSQL-схеме, созданной в init-db.sql.
// @Getter/@Setter — Lombok генерирует геттеры и сеттеры для всех полей.
// @NoArgsConstructor — обязателен для JPA: Hibernate создаёт объект пустым конструктором.
// @AllArgsConstructor — нужен для совместной работы с @Builder.
// @Builder — паттерн "строитель": Customer.builder().email(...).role(...).build()
//            делает создание объекта читаемым без длинного конструктора.
@Entity
@Table(name = "customers", schema = "customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    // @Id                 — первичный ключ таблицы.
    // @GeneratedValue     — стратегия IDENTITY: БД сама назначает id (SERIAL/BIGSERIAL в PostgreSQL).
    //                       После save() Hibernate возвращает объект с заполненным id.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true  — добавляет UNIQUE constraint на колонку; нельзя зарегистрировать один email дважды.
    // nullable = false — добавляет NOT NULL constraint; Hibernate также проверяет это перед INSERT.
    @Column(unique = true, nullable = false)
    private String email;

    // Пароль хранится как bcrypt-хэш (60 символов), а не открытым текстом.
    // BCrypt: "$2a$10$..." — алгоритм, стоимость (cost factor), соль, хэш — всё в одной строке.
    // Проверка: passwordEncoder.matches(rawPassword, passwordHash) — не нужно хранить соль отдельно.
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    // Телефон необязателен (нет nullable = false) → колонка допускает NULL.
    private String phone;

    // @Enumerated(EnumType.STRING) — хранить роль как строку ("CUSTOMER"), а не как число (0, 1, 2).
    // EnumType.ORDINAL (по умолчанию) опасен: добавление новой константы в середину enum
    // сместит все числовые значения и испортит данные в БД.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
