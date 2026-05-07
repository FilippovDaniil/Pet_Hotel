package com.pethotel.support.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// @Entity — сообщает Hibernate, что этот класс соответствует таблице в БД
// @Table — явно задаём имя таблицы и схему БД (у каждого сервиса своя схема)
@Entity
@Table(name = "messages", schema = "support")
// Lombok-аннотации — генерируют методы во время компиляции:
@Getter          // генерирует getX() для всех полей
@Setter          // генерирует setX() для всех полей
@NoArgsConstructor   // генерирует конструктор без аргументов (нужен Hibernate для создания объектов)
@AllArgsConstructor  // генерирует конструктор со всеми аргументами (нужен @Builder)
@Builder             // генерирует паттерн Builder: SupportMessage.builder().field(val).build()
public class SupportMessage {

    // @Id — первичный ключ таблицы
    // @GeneratedValue(IDENTITY) — БД сама генерирует id (PostgreSQL: SERIAL / BIGSERIAL)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // customerId — не внешний ключ (@ManyToOne), а просто Long.
    // Сервисы в микросервисной архитектуре не имеют общих JPA-связей между схемами.
    // Данные пользователя живут в схеме customer — мы только храним его id здесь.
    @Column(nullable = false)
    private Long customerId;

    // Дублируем email клиента прямо в таблицу сообщений.
    // Это денормализация, но без неё admin при просмотре диалогов не узнает email
    // (customer-service — отдельный сервис, каждый лишний HTTP-вызов = лишняя зависимость).
    @Column(nullable = false)
    private String customerEmail;

    // Кто отправил сообщение: "CUSTOMER" или "ADMIN".
    // Используем String, а не enum, чтобы не тащить общую зависимость ради одного поля.
    @Column(nullable = false)
    private String senderRole;

    // columnDefinition = "TEXT" — PostgreSQL тип TEXT (без ограничения длины).
    // По умолчанию Hibernate создал бы VARCHAR(255), что мало для сообщения поддержки.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // LocalDateTime без @Column — Hibernate создаст колонку с именем created_at (snake_case).
    // Всегда храним UTC, форматирование — забота фронтенда.
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Два флага прочтения — позволяют показывать статус доставки ("✓" / "✓✓"):
    // readByCustomer = true: клиент видел этот ответ от admin
    // readByAdmin = true: admin видел это сообщение от клиента

    // @Builder.Default — без этой аннотации Builder установил бы false (Java default),
    // но явная запись делает намерение очевидным и не зависит от поведения Lombok по умолчанию.
    @Column(nullable = false)
    @Builder.Default
    private boolean readByCustomer = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean readByAdmin = false;
}
