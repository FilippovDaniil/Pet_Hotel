package com.pethotel.amenity.config;

import com.pethotel.amenity.entity.Amenity;
import com.pethotel.amenity.repository.AmenityRepository;
import com.pethotel.common.enums.ServiceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final AmenityRepository amenityRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (amenityRepository.count() > 0) return;

        amenityRepository.saveAll(List.of(
            amenity("Финская сауна",              ServiceType.SAUNA,        "2000", 120),
            amenity("Русская баня",               ServiceType.BATH,         "2000", 120),
            amenity("Бассейн с подогревом",       ServiceType.POOL,         "500",  60),
            amenity("Бильярд (русский)",          ServiceType.BILLIARD_RUS, "600",  60),
            amenity("Бильярд (американский пул)", ServiceType.BILLIARD_US,  "600",  60),
            amenity("Классический массаж",        ServiceType.MASSAGE,      "3000", 60)
        ));

        log.info("Seeded {} amenities", amenityRepository.count());
    }

    private Amenity amenity(String name, ServiceType type, String price, int maxMinutes) {
        return Amenity.builder()
                .name(name)
                .type(type)
                .defaultPrice(new BigDecimal(price))
                .maxDurationMinutes(maxMinutes)
                .build();
    }
}
