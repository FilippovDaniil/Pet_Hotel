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
            amenity("Финская сауна",              ServiceType.SAUNA,        "2000", 120,
                "Финская сауна с парилкой, бассейном для охлаждения и зоной отдыха. Максимальная температура 90°C. Вместимость до 8 человек."),
            amenity("Русская баня",               ServiceType.BATH,         "2000", 120,
                "Традиционная русская баня на дровах с берёзовыми вениками. Идеально для восстановления после дороги. Вместимость до 6 человек."),
            amenity("Бассейн с подогревом",       ServiceType.POOL,         "500",  60,
                "Подогреваемый бассейн 25×10 м с детской зоной. Температура воды 28°C. Работает ежедневно с 8:00 до 22:00."),
            amenity("Бильярд (русский)",          ServiceType.BILLIARD_RUS, "600",  60,
                "Стол для русского бильярда — классика отдыха. Кии, мелок и шары предоставляются. Зал рассчитан на 2 стола."),
            amenity("Бильярд (американский пул)", ServiceType.BILLIARD_US,  "600",  60,
                "Стол для американского пула (8-ball / 9-ball). Кии и полный комплект шаров предоставляются."),
            amenity("Классический массаж",        ServiceType.MASSAGE,      "3000", 60,
                "Классический расслабляющий массаж от профессиональных массажистов. Продолжительность 60 минут. Предварительная запись обязательна.")
        ));

        log.info("Seeded {} amenities", amenityRepository.count());
    }

    private Amenity amenity(String name, ServiceType type, String price, int maxMinutes, String description) {
        return Amenity.builder()
                .name(name)
                .type(type)
                .defaultPrice(new BigDecimal(price))
                .maxDurationMinutes(maxMinutes)
                .description(description)
                .available(true)
                .build();
    }
}
