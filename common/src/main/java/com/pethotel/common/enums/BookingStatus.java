package com.pethotel.common.enums;

// Жизненный цикл бронирования — конечный автомат с четырьмя состояниями.
// Допустимые переходы (enforced в BookingService):
//
//   PENDING ──confirm──► CONFIRMED ──checkOut──► COMPLETED
//      │                     │
//      └──cancel──► CANCELLED ◄──cancel──┘
//
// Детали переходов:
//   confirm  — только из PENDING (reception подтверждает заезд)
//   cancel   — только из PENDING или CONFIRMED; штраф 30% если заезд в течение 24 ч
//   checkIn  — только из CONFIRMED (физическое заселение, не меняет статус)
//   checkOut — только из CONFIRMED → устанавливает COMPLETED и запускает выставление счёта
public enum BookingStatus {
    PENDING,    // создано клиентом, ждёт подтверждения ресепшн
    CONFIRMED,  // ресепшн подтвердил; клиент уже заселён или ещё едет
    CANCELLED,  // отменено (клиентом или ресепшн); номер освобождён в room-service через Kafka
    COMPLETED   // выезд оформлен; billing-service создаёт итоговый счёт
}
