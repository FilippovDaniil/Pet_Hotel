package com.pethotel.amenity.dto;

import com.pethotel.common.enums.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

// DTO для создания и обновления услуги (POST/PUT /api/amenities).
// Изображение загружается отдельным запросом (POST /{id}/image, multipart/form-data) —
// смешивать JSON и бинарные данные в одном запросе неудобно для клиентов.
@Data
public class AmenityRequest {

    // message = "..." — кастомное сообщение об ошибке вместо стандартного.
    // Попадёт в ответ GlobalExceptionHandler: {"error": "Name must not be blank"}.
    @NotBlank(message = "Name must not be blank")
    private String name;

    @NotNull(message = "Type must not be null")
    private ServiceType type;

    @NotNull(message = "Default price must not be null")
    @DecimalMin(value = "0.01", message = "Default price must be at least 0.01")
    private BigDecimal defaultPrice;

    @Min(value = 1, message = "Max duration must be at least 1 minute")
    private int maxDurationMinutes;

    private String description;

    // available = true по умолчанию — новая услуга сразу активна.
    // Клиент может явно передать false, если услуга добавляется как "скоро откроется".
    private boolean available = true;
}
