package com.pethotel.common.enums;

// Типы дополнительных услуг, доступных при бронировании.
// Цена и скидки зависят от класса номера (RoomClass) — см. AmenityPriceCalculator в booking-service:
//
//   SAUNA        — 2000 руб (ORDINARY), 1400 (MIDDLE), 0 первый раз (PREMIUM)
//   BATH         — 2000 руб (ORDINARY), 1400 (MIDDLE), 0 первый раз (PREMIUM);
//                  SAUNA и BATH делят одну бесплатную квоту у PREMIUM-гостей
//   POOL         — 500 руб (ORDINARY), 350 (MIDDLE), 0 всегда (PREMIUM)
//   BILLIARD_RUS — 600 руб для всех классов (русский бильярд)
//   BILLIARD_US  — 600 руб для всех классов (американский пул)
//   MASSAGE      — 3000 руб (ORDINARY/MIDDLE), 0 первый раз (PREMIUM)
//
// Хранится как строка в БД (EnumType.STRING в entity AmenityBooking).
// Передаётся в ServiceBookedEvent → amenity-service для учёта загруженности.
public enum ServiceType {
    SAUNA,
    BATH,
    POOL,
    BILLIARD_RUS,
    BILLIARD_US,
    MASSAGE
}
