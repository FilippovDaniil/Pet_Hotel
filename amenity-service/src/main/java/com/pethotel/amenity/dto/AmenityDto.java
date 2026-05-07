package com.pethotel.amenity.dto;

import com.pethotel.common.enums.ServiceType;
import lombok.Data;

import java.math.BigDecimal;

// DTO ответа для одной услуги.
// Намеренно не включает imageData (byte[]): передавать бинарные данные в JSON неэффективно
// (Base64 увеличивает размер на ~33%). Изображение доступно отдельным эндпоинтом GET /{id}/image.
// hasImage = true/false сигнализирует фронту, нужно ли показывать кнопку "Посмотреть фото".
@Data
public class AmenityDto {
    private Long id;
    private String name;
    private ServiceType type;
    private BigDecimal defaultPrice;    // базовая цена (без учёта класса номера)
    private int maxDurationMinutes;
    private String description;
    private boolean available;
    private boolean hasImage;           // есть ли загруженное изображение; imageData не передаётся
}
