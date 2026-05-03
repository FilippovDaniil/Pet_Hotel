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
@Component
public class AmenityPriceCalculator {

    private static final BigDecimal SAUNA_PRICE    = new BigDecimal("2000");
    private static final BigDecimal BATH_PRICE     = new BigDecimal("2000");
    private static final BigDecimal POOL_PRICE     = new BigDecimal("500");
    private static final BigDecimal BILLIARD_PRICE = new BigDecimal("600");
    private static final BigDecimal MASSAGE_PRICE  = new BigDecimal("3000");
    private static final BigDecimal MIDDLE_DISCOUNT = new BigDecimal("0.70");

    public BigDecimal calculatePrice(ServiceType type, RoomClass roomClass, Booking booking) {
        BigDecimal base = basePrice(type);
        return switch (roomClass) {
            case ORDINARY -> base;
            case MIDDLE -> applyMiddleDiscount(type, base);
            case PREMIUM -> applyPremiumDiscount(type, base, booking);
        };
    }

    private BigDecimal basePrice(ServiceType type) {
        return switch (type) {
            case SAUNA -> SAUNA_PRICE;
            case BATH -> BATH_PRICE;
            case POOL -> POOL_PRICE;
            case BILLIARD_RUS, BILLIARD_US -> BILLIARD_PRICE;
            case MASSAGE -> MASSAGE_PRICE;
        };
    }

    private BigDecimal applyMiddleDiscount(ServiceType type, BigDecimal base) {
        if (type == ServiceType.SAUNA || type == ServiceType.BATH || type == ServiceType.POOL) {
            return base.multiply(MIDDLE_DISCOUNT);
        }
        return base;
    }

    private BigDecimal applyPremiumDiscount(ServiceType type, BigDecimal base, Booking booking) {
        return switch (type) {
            case POOL -> BigDecimal.ZERO;
            case SAUNA, BATH -> hasFreeSaunaOrBath(booking) ? BigDecimal.ZERO : base;
            case MASSAGE -> hasFreeMassage(booking) ? BigDecimal.ZERO : base;
            default -> base;
        };
    }

    private boolean hasFreeSaunaOrBath(Booking booking) {
        return booking.getAmenities().stream()
                .noneMatch(a -> (a.getServiceType() == ServiceType.SAUNA || a.getServiceType() == ServiceType.BATH)
                        && a.getPrice().compareTo(BigDecimal.ZERO) == 0);
    }

    private boolean hasFreeMassage(Booking booking) {
        return booking.getAmenities().stream()
                .noneMatch(a -> a.getServiceType() == ServiceType.MASSAGE
                        && a.getPrice().compareTo(BigDecimal.ZERO) == 0);
    }
}
