package com.pethotel.booking.service;

import com.pethotel.booking.entity.Booking;
import com.pethotel.booking.entity.BookingAmenity;
import com.pethotel.common.enums.BookingStatus;
import com.pethotel.common.enums.RoomClass;
import com.pethotel.common.enums.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Unit-тест AmenityPriceCalculator: проверяет все комбинации ServiceType × RoomClass.
// Нет Spring-контекста — тест мгновенный, проверяет только математику привилегий.
// @Nested — JUnit 5: вложенные классы группируют тесты по классу номера (ORDINARY/MIDDLE/PREMIUM).
class AmenityPriceCalculatorTest {

    private AmenityPriceCalculator calculator;

    // Создаём свежий калькулятор перед каждым тестом.
    @BeforeEach
    void setUp() {
        calculator = new AmenityPriceCalculator();
    }

    // ── ORDINARY ─────────────────────────────────────────────────────────────────
    // Стандарт: никаких скидок, все услуги по базовой цене.

    @Nested
    class Ordinary {

        @Test void sauna_fullPrice() {
            assertPrice(ServiceType.SAUNA, RoomClass.ORDINARY, emptyBooking(), "2000");
        }

        @Test void bath_fullPrice() {
            assertPrice(ServiceType.BATH, RoomClass.ORDINARY, emptyBooking(), "2000");
        }

        @Test void pool_fullPrice() {
            assertPrice(ServiceType.POOL, RoomClass.ORDINARY, emptyBooking(), "500");
        }

        @Test void billiardRus_fullPrice() {
            assertPrice(ServiceType.BILLIARD_RUS, RoomClass.ORDINARY, emptyBooking(), "600");
        }

        @Test void billiardUs_fullPrice() {
            assertPrice(ServiceType.BILLIARD_US, RoomClass.ORDINARY, emptyBooking(), "600");
        }

        @Test void massage_fullPrice() {
            assertPrice(ServiceType.MASSAGE, RoomClass.ORDINARY, emptyBooking(), "3000");
        }
    }

    // ── MIDDLE ───────────────────────────────────────────────────────────────────
    // Скидка 30% только на термальные услуги: SAUNA 2000→1400, BATH 2000→1400, POOL 500→350.
    // BILLIARD и MASSAGE — без скидки.

    @Nested
    class Middle {

        @Test void sauna_30PercentDiscount() {
            assertPrice(ServiceType.SAUNA, RoomClass.MIDDLE, emptyBooking(), "1400"); // 2000 × 0.70
        }

        @Test void bath_30PercentDiscount() {
            assertPrice(ServiceType.BATH, RoomClass.MIDDLE, emptyBooking(), "1400");
        }

        @Test void pool_30PercentDiscount() {
            assertPrice(ServiceType.POOL, RoomClass.MIDDLE, emptyBooking(), "350");  // 500 × 0.70
        }

        @Test void billiard_noDiscount() {
            assertPrice(ServiceType.BILLIARD_RUS, RoomClass.MIDDLE, emptyBooking(), "600"); // без скидки
        }

        @Test void massage_noDiscount() {
            assertPrice(ServiceType.MASSAGE, RoomClass.MIDDLE, emptyBooking(), "3000");
        }
    }

    // ── PREMIUM ──────────────────────────────────────────────────────────────────
    // Сложные правила: POOL — всегда бесплатен; SAUNA/BATH — делят одну бесплатную квоту;
    // MASSAGE — своя квота; BILLIARD — без привилегий даже для PREMIUM.

    @Nested
    class Premium {

        // POOL бесплатен всегда, даже если квота SAUNA/BATH уже использована.
        @Test void pool_alwaysFree() {
            Booking booking = bookingWithAmenity(ServiceType.SAUNA, BigDecimal.ZERO); // квота использована
            assertPrice(ServiceType.POOL, RoomClass.PREMIUM, booking, "0"); // POOL всё равно 0
        }

        // Первая SAUNA — бесплатна (квота не использована).
        @Test void sauna_firstIsFree() {
            assertPrice(ServiceType.SAUNA, RoomClass.PREMIUM, emptyBooking(), "0");
        }

        // Вторая SAUNA — платная (в booking уже есть бесплатная SAUNA → квота занята).
        @Test void sauna_paidAfterFreeQuotaUsed() {
            Booking booking = bookingWithAmenity(ServiceType.SAUNA, BigDecimal.ZERO); // квота занята
            assertPrice(ServiceType.SAUNA, RoomClass.PREMIUM, booking, "2000");
        }

        // Первая BATH — бесплатна (квота пустая).
        @Test void bath_freeWhenQuotaNotUsed() {
            assertPrice(ServiceType.BATH, RoomClass.PREMIUM, emptyBooking(), "0");
        }

        // Если SAUNA уже использовала квоту — BATH платная (они делят одну квоту!).
        @Test void bath_paidWhenSaunaAlreadyUsedQuota() {
            Booking booking = bookingWithAmenity(ServiceType.SAUNA, BigDecimal.ZERO);
            assertPrice(ServiceType.BATH, RoomClass.PREMIUM, booking, "2000");
        }

        // Если BATH уже использовала квоту — ещё одна BATH платная.
        @Test void bath_paidWhenBathAlreadyUsedQuota() {
            Booking booking = bookingWithAmenity(ServiceType.BATH, BigDecimal.ZERO);
            assertPrice(ServiceType.BATH, RoomClass.PREMIUM, booking, "2000");
        }

        // Если BATH использовала квоту — SAUNA тоже платная (общая квота).
        @Test void sauna_paidWhenBathAlreadyUsedQuota() {
            Booking booking = bookingWithAmenity(ServiceType.BATH, BigDecimal.ZERO);
            assertPrice(ServiceType.SAUNA, RoomClass.PREMIUM, booking, "2000");
        }

        // Пошаговый тест: первая SAUNA = 0, добавляем её в booking, вторая BATH = 2000.
        @Test void saunaAndBath_onlyFirstIsFreeThenPaid() {
            Booking booking = emptyBooking();
            // Первый расчёт: квота свободна → бесплатно
            BigDecimal firstPrice = calculator.calculatePrice(ServiceType.SAUNA, RoomClass.PREMIUM, booking);
            assertThat(firstPrice).isEqualByComparingTo("0");

            // Добавляем SAUNA с price=0 в коллекцию → квота занята
            booking.getAmenities().add(freeAmenity(ServiceType.SAUNA));
            // Второй расчёт: квота занята → платно
            BigDecimal secondPrice = calculator.calculatePrice(ServiceType.BATH, RoomClass.PREMIUM, booking);
            assertThat(secondPrice).isEqualByComparingTo("2000");
        }

        @Test void massage_firstIsFree() {
            assertPrice(ServiceType.MASSAGE, RoomClass.PREMIUM, emptyBooking(), "0");
        }

        // После бесплатного массажа — следующий платный.
        @Test void massage_paidAfterFreeUsed() {
            Booking booking = bookingWithAmenity(ServiceType.MASSAGE, BigDecimal.ZERO);
            assertPrice(ServiceType.MASSAGE, RoomClass.PREMIUM, booking, "3000");
        }

        // Квота MASSAGE и квота SAUNA/BATH независимы: использованная SAUNA не влияет на MASSAGE.
        @Test void massage_quotaIndependentOfSaunaQuota() {
            Booking booking = bookingWithAmenity(ServiceType.SAUNA, BigDecimal.ZERO);
            assertPrice(ServiceType.MASSAGE, RoomClass.PREMIUM, booking, "0"); // квота MASSAGE свободна
        }

        // Бильярд — 600 руб для всех классов, PREMIUM не исключение.
        @Test void billiard_alwaysPaidForPremium() {
            assertPrice(ServiceType.BILLIARD_RUS, RoomClass.PREMIUM, emptyBooking(), "600");
            assertPrice(ServiceType.BILLIARD_US, RoomClass.PREMIUM, emptyBooking(), "600");
        }

        // Важно: квота "сгорает" только при price == 0.
        // Если SAUNA добавлена по полной цене (2000), квота остаётся свободной.
        @Test void paidAmenitiesDoNotConsumeQuota() {
            Booking booking = bookingWithAmenity(ServiceType.SAUNA, new BigDecimal("2000")); // платная, не бесплатная
            assertPrice(ServiceType.SAUNA, RoomClass.PREMIUM, booking, "0"); // квота всё ещё свободна
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    // isEqualByComparingTo: сравниваем BigDecimal по значению (0 == 0.00 == 0.0).
    private void assertPrice(ServiceType type, RoomClass roomClass, Booking booking, String expected) {
        assertThat(calculator.calculatePrice(type, roomClass, booking))
                .isEqualByComparingTo(expected);
    }

    // Бронирование без услуг — отправная точка для большинства тестов.
    private Booking emptyBooking() {
        return Booking.builder()
                .id(1L).customerId(1L).roomId(1L)
                .checkInDate(LocalDate.now()).checkOutDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.PENDING).totalPrice(BigDecimal.ZERO)
                .build(); // amenities = [] (пустой ArrayList из @Builder.Default)
    }

    // Бронирование с одной уже добавленной услугой — для тестов "квота занята".
    private Booking bookingWithAmenity(ServiceType type, BigDecimal price) {
        Booking booking = emptyBooking();
        booking.getAmenities().add(freeAmenity(type, price)); // имитируем уже добавленную услугу
        return booking;
    }

    // Перегрузка: по умолчанию price = 0 (бесплатная квота).
    private BookingAmenity freeAmenity(ServiceType type) {
        return freeAmenity(type, BigDecimal.ZERO);
    }

    private BookingAmenity freeAmenity(ServiceType type, BigDecimal price) {
        return BookingAmenity.builder()
                .serviceType(type)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .price(price)
                .build();
    }
}
