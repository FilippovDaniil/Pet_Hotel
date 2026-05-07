package com.pethotel.booking.dto;

import com.pethotel.common.enums.ServiceType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

// DTO одной дополнительной услуги в запросе на бронирование.
// Цена не передаётся клиентом: она рассчитывается сервером через AmenityPriceCalculator
// на основании класса номера — клиент не может задать её самостоятельно.
@Data
public class AmenityBookingRequest {

    @NotNull
    private ServiceType serviceType;  // какая услуга: SAUNA, MASSAGE и т.д.

    @NotNull
    private LocalDateTime startTime;  // начало слота

    @NotNull
    private LocalDateTime endTime;    // конец слота
}
