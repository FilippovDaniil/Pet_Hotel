package com.pethotel.amenity.entity;

import com.pethotel.common.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// Справочная запись одной дополнительной услуги отеля.
// Связь с бронированием — через ServiceType enum, а не Foreign Key:
//   booking-service хранит booking_amenities.service_type, а amenity-service — независимый справочник.
@Entity
@Table(name = "amenities", schema = "amenity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;  // отображаемое название: "Финская сауна", "Классический массаж"

    // ServiceType enum — связывает запись справочника с константой в booking-service.
    // Позволяет искать услугу по типу: findByType(ServiceType.SAUNA).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType type;

    // precision = 10, scale = 2 — PostgreSQL NUMERIC(10, 2): 10 значимых цифр, 2 после запятой.
    // Это точный денежный тип; в отличие от FLOAT/DOUBLE не имеет ошибок округления.
    // defaultPrice — базовая цена из справочника (фактическая цена рассчитывается в AmenityPriceCalculator).
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultPrice;

    // Максимальная продолжительность слота в минутах — используется на фронте для UI выбора времени.
    @Column(nullable = false)
    private int maxDurationMinutes;

    // length = 2000 — увеличенный лимит VARCHAR для длинного описания услуги.
    // По умолчанию @Column строки — VARCHAR(255), которого недостаточно для детального описания.
    @Column(length = 2000)
    private String description;

    // columnDefinition = "boolean default true" — DDL-уровень: если поле не передано при INSERT,
    // БД подставит true. @Builder.Default — для builder-паттерна: по умолчанию услуга активна.
    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean available = true;

    // columnDefinition = "bytea" — PostgreSQL-тип для хранения бинарных данных (изображений).
    // Альтернатива: хранить файл на S3/MinIO и держать только URL. bytea проще для учебного проекта,
    // но не масштабируется при большом объёме изображений.
    @Column(columnDefinition = "bytea")
    private byte[] imageData;

    // MIME-тип изображения: "image/jpeg", "image/png" и т.д.
    // Нужен для корректного заголовка Content-Type в ответе GET /{id}/image.
    @Column(length = 100)
    private String imageMimeType;
}
