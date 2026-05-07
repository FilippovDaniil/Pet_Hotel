package com.pethotel.common.enums;

// Класс номера определяет ценовую категорию и привилегии при бронировании.
// Используется в нескольких сервисах:
//   - room-service:    хранит roomClass в сущности Room
//   - booking-service: получает roomClass через WebClient → room-service,
//                      передаёт в AmenityPriceCalculator для расчёта скидок
//   - dining-service:  получает roomClass через WebClient → booking-service,
//                      использует в DailyLimitService для дневного лимита буфета
//   - billing-service: принимает roomClass в BookingCreatedEvent / BookingCompletedEvent
public enum RoomClass {
    ORDINARY,  // стандарт: нет скидок на услуги, лимит буфета 0 руб/день
    MIDDLE,    // средний:  скидка 30% на SAUNA/BATH/POOL, лимит буфета 1000 руб/день
    PREMIUM    // премиум:  первая SAUNA/BATH/MASSAGE бесплатна, POOL всегда бесплатен, лимит 3000 руб/день
}
