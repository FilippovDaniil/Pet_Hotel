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

class AmenityPriceCalculatorTest {

    private AmenityPriceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new AmenityPriceCalculator();
    }

    // ── ORDINARY ─────────────────────────────────────────────────────────────────

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

    @Nested
    class Middle {

        @Test void sauna_30PercentDiscount() {
            assertPrice(ServiceType.SAUNA, RoomClass.MIDDLE, emptyBooking(), "1400");
        }

        @Test void bath_30PercentDiscount() {
            assertPrice(ServiceType.BATH, RoomClass.MIDDLE, emptyBooking(), "1400");
        }

        @Test void pool_30PercentDiscount() {
            assertPrice(ServiceType.POOL, RoomClass.MIDDLE, emptyBooking(), "350");
        }

        @Test void billiard_noDiscount() {
            assertPrice(ServiceType.BILLIARD_RUS, RoomClass.MIDDLE, emptyBooking(), "600");
        }

        @Test void massage_noDiscount() {
            assertPrice(ServiceType.MASSAGE, RoomClass.MIDDLE, emptyBooking(), "3000");
        }
    }

    // ── PREMIUM ──────────────────────────────────────────────────────────────────

    @Nested
    class Premium {

        @Test void pool_alwaysFree() {
            // Pool is free even if the sauna/bath quota is used
            Booking booking = bookingWithAmenity(ServiceType.SAUNA, BigDecimal.ZERO);
            assertPrice(ServiceType.POOL, RoomClass.PREMIUM, booking, "0");
        }

        @Test void sauna_firstIsFree() {
            assertPrice(ServiceType.SAUNA, RoomClass.PREMIUM, emptyBooking(), "0");
        }

        @Test void sauna_paidAfterFreeQuotaUsed() {
            Booking booking = bookingWithAmenity(ServiceType.SAUNA, BigDecimal.ZERO);
            assertPrice(ServiceType.SAUNA, RoomClass.PREMIUM, booking, "2000");
        }

        @Test void bath_freeWhenQuotaNotUsed() {
            assertPrice(ServiceType.BATH, RoomClass.PREMIUM, emptyBooking(), "0");
        }

        @Test void bath_paidWhenSaunaAlreadyUsedQuota() {
            // SAUNA and BATH share one free quota — if SAUNA was free, BATH is paid
            Booking booking = bookingWithAmenity(ServiceType.SAUNA, BigDecimal.ZERO);
            assertPrice(ServiceType.BATH, RoomClass.PREMIUM, booking, "2000");
        }

        @Test void bath_paidWhenBathAlreadyUsedQuota() {
            Booking booking = bookingWithAmenity(ServiceType.BATH, BigDecimal.ZERO);
            assertPrice(ServiceType.BATH, RoomClass.PREMIUM, booking, "2000");
        }

        @Test void sauna_paidWhenBathAlreadyUsedQuota() {
            Booking booking = bookingWithAmenity(ServiceType.BATH, BigDecimal.ZERO);
            assertPrice(ServiceType.SAUNA, RoomClass.PREMIUM, booking, "2000");
        }

        @Test void saunaAndBath_onlyFirstIsFreeThenPaid() {
            // First sauna → free (quota used), second bath → paid
            Booking booking = emptyBooking();
            BigDecimal firstPrice = calculator.calculatePrice(ServiceType.SAUNA, RoomClass.PREMIUM, booking);
            assertThat(firstPrice).isEqualByComparingTo("0");

            booking.getAmenities().add(freeAmenity(ServiceType.SAUNA));
            BigDecimal secondPrice = calculator.calculatePrice(ServiceType.BATH, RoomClass.PREMIUM, booking);
            assertThat(secondPrice).isEqualByComparingTo("2000");
        }

        @Test void massage_firstIsFree() {
            assertPrice(ServiceType.MASSAGE, RoomClass.PREMIUM, emptyBooking(), "0");
        }

        @Test void massage_paidAfterFreeUsed() {
            Booking booking = bookingWithAmenity(ServiceType.MASSAGE, BigDecimal.ZERO);
            assertPrice(ServiceType.MASSAGE, RoomClass.PREMIUM, booking, "3000");
        }

        @Test void massage_quotaIndependentOfSaunaQuota() {
            // Both SAUNA and MASSAGE quotas are independent of each other
            Booking booking = bookingWithAmenity(ServiceType.SAUNA, BigDecimal.ZERO);
            assertPrice(ServiceType.MASSAGE, RoomClass.PREMIUM, booking, "0");
        }

        @Test void billiard_alwaysPaidForPremium() {
            assertPrice(ServiceType.BILLIARD_RUS, RoomClass.PREMIUM, emptyBooking(), "600");
            assertPrice(ServiceType.BILLIARD_US, RoomClass.PREMIUM, emptyBooking(), "600");
        }

        @Test void paidAmenitiesDoNotConsumeQuota() {
            // If SAUNA was added at full price (not free), quota is still available
            Booking booking = bookingWithAmenity(ServiceType.SAUNA, new BigDecimal("2000"));
            assertPrice(ServiceType.SAUNA, RoomClass.PREMIUM, booking, "0");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private void assertPrice(ServiceType type, RoomClass roomClass, Booking booking, String expected) {
        assertThat(calculator.calculatePrice(type, roomClass, booking))
                .isEqualByComparingTo(expected);
    }

    private Booking emptyBooking() {
        return Booking.builder()
                .id(1L).customerId(1L).roomId(1L)
                .checkInDate(LocalDate.now()).checkOutDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.PENDING).totalPrice(BigDecimal.ZERO)
                .build();
    }

    private Booking bookingWithAmenity(ServiceType type, BigDecimal price) {
        Booking booking = emptyBooking();
        booking.getAmenities().add(freeAmenity(type, price));
        return booking;
    }

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
