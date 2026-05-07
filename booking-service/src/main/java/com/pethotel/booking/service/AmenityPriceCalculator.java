package com.pethotel.booking.service;

import com.pethotel.booking.entity.Booking;
import com.pethotel.common.enums.RoomClass;
import com.pethotel.common.enums.ServiceType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Calculates amenity price based on room class privileges.
 *
 * ORDINARY: all amenities paid at full price, no buffet limit.
 * MIDDLE:   SAUNA/BATH/POOL with 30% discount.
 * PREMIUM:  POOL free, one SAUNA or BATH free, one MASSAGE free.
 */
// @Component (не @Service) — класс содержит только расчётную логику без транзакций и репозиториев.
// Намеренно вынесен из BookingService: изолирует сложную логику ценообразования и упрощает тестирование.
@Component
public class AmenityPriceCalculator {

    // Базовые цены — константы класса (не БД): в учебном проекте цены фиксированы.
    // В production-системе их стоит хранить в таблице AmenityPrice или amenity-service.
    private static final BigDecimal SAUNA_PRICE    = new BigDecimal("2000");
    private static final BigDecimal BATH_PRICE     = new BigDecimal("2000");
    private static final BigDecimal POOL_PRICE     = new BigDecimal("500");
    private static final BigDecimal BILLIARD_PRICE = new BigDecimal("600");
    private static final BigDecimal MASSAGE_PRICE  = new BigDecimal("3000");
    // Коэффициент скидки для MIDDLE: 0.70 означает "70% от базовой цены" (скидка 30%).
    private static final BigDecimal MIDDLE_DISCOUNT = new BigDecimal("0.70");

    // Точка входа: определяет итоговую цену по типу услуги и классу номера.
    // booking передаётся для PREMIUM: нужно знать, использовал ли гость уже бесплатную квоту.
    public BigDecimal calculatePrice(ServiceType type, RoomClass roomClass, Booking booking) {
        BigDecimal base = basePrice(type);
        // Java 14+ switch expression — компактнее, чем if-else; компилятор требует покрыть все варианты enum.
        return switch (roomClass) {
            case ORDINARY -> base;                               // без скидок
            case MIDDLE   -> applyMiddleDiscount(type, base);   // -30% на SAUNA/BATH/POOL
            case PREMIUM  -> applyPremiumDiscount(type, base, booking); // комплексные привилегии
        };
    }

    // Возвращает базовую цену по типу услуги.
    private BigDecimal basePrice(ServiceType type) {
        return switch (type) {
            case SAUNA          -> SAUNA_PRICE;
            case BATH           -> BATH_PRICE;
            case POOL           -> POOL_PRICE;
            // BILLIARD_RUS и BILLIARD_US имеют одинаковую цену — объединены в одну ветку.
            case BILLIARD_RUS, BILLIARD_US -> BILLIARD_PRICE;
            case MASSAGE        -> MASSAGE_PRICE;
        };
    }

    // MIDDLE: скидка 30% только на термальные услуги. Бильярд и массаж — без скидки.
    private BigDecimal applyMiddleDiscount(ServiceType type, BigDecimal base) {
        if (type == ServiceType.SAUNA || type == ServiceType.BATH || type == ServiceType.POOL) {
            // multiply(0.70): BigDecimal-арифметика точная — нет ошибок округления float.
            return base.multiply(MIDDLE_DISCOUNT);
        }
        return base;
    }

    // PREMIUM: сложная логика с квотами.
    private BigDecimal applyPremiumDiscount(ServiceType type, BigDecimal base, Booking booking) {
        return switch (type) {
            // POOL всегда бесплатен для PREMIUM-гостей.
            case POOL -> BigDecimal.ZERO;
            // SAUNA и BATH делят одну бесплатную квоту: первый заказ любого из двух = 0 руб.
            // hasFreeSaunaOrBath() ищет в уже добавленных услугах запись с price == 0.
            // Если такой нет — это первый заказ, даём бесплатно; иначе — полная цена.
            case SAUNA, BATH -> hasFreeSaunaOrBath(booking) ? BigDecimal.ZERO : base;
            // MASSAGE: одна бесплатная квота независимо от SAUNA/BATH.
            case MASSAGE -> hasFreeMassage(booking) ? BigDecimal.ZERO : base;
            // Бильярд — без привилегий даже для PREMIUM.
            default -> base;
        };
    }

    // Проверяет: есть ли в текущем бронировании уже бесплатная SAUNA или BATH?
    // noneMatch — возвращает true (= "нет бесплатной квоты использовано") если ни одна запись не совпадает.
    // Логика: "бесплатная квота доступна" ↔ "нет элемента amenity с (SAUNA или BATH) и price == 0"
    private boolean hasFreeSaunaOrBath(Booking booking) {
        return booking.getAmenities().stream()
                .noneMatch(a -> (a.getServiceType() == ServiceType.SAUNA || a.getServiceType() == ServiceType.BATH)
                        && a.getPrice().compareTo(BigDecimal.ZERO) == 0);
    }

    // Аналогично для MASSAGE.
    private boolean hasFreeMassage(Booking booking) {
        return booking.getAmenities().stream()
                .noneMatch(a -> a.getServiceType() == ServiceType.MASSAGE
                        && a.getPrice().compareTo(BigDecimal.ZERO) == 0);
    }
}
