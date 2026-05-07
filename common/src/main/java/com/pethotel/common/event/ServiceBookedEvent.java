package com.pethotel.common.event;

import com.pethotel.common.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Kafka-событие: клиент забронировал дополнительную услугу (сауна, массаж и т.д.).
//
// Producer:  booking-service (при добавлении amenity к бронированию)
// Consumers:
//   - amenity-service → учитывает занятость слота; может проверять пересечения по времени
//
// startTime/endTime — конкретный временной слот.
// price — итоговая цена с учётом скидки по классу номера (уже рассчитана в AmenityPriceCalculator).
//
// Обратите внимание: billing-service на этот топик НЕ подписан.
// Стоимость услуг накапливается в booking.amenities и передаётся в BookingCompletedEvent.amenitiesTotal.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBookedEvent {
    private Long bookingId;
    private Long customerId;
    private ServiceType serviceType;   // SAUNA, BATH, POOL и т.д.
    private LocalDateTime startTime;   // начало слота
    private LocalDateTime endTime;     // конец слота
    private BigDecimal price;          // финальная цена (может быть 0 для PREMIUM)
}
