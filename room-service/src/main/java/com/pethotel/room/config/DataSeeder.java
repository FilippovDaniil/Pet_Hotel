package com.pethotel.room.config;

import com.pethotel.common.enums.RoomClass;
import com.pethotel.room.entity.Room;
import com.pethotel.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// Заполняет БД тестовыми номерами при первом запуске.
// 10 номеров: 4×ORDINARY, 3×MIDDLE, 3×PREMIUM — разный диапазон цен и вместимости.
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final RoomRepository roomRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        // count() > 0 — идемпотентная проверка: если номера уже есть, не добавляем дубликаты.
        // Данные сохраняются в PostgreSQL volume → docker-compose down (без -v) их не удаляет.
        if (roomRepository.count() > 0) return;

        // saveAll() — один batch INSERT вместо 10 отдельных; эффективнее при старте.
        roomRepository.saveAll(List.of(
            // ORDINARY: 2900–3500 руб/ночь, 2–3 гостя
            room("101", RoomClass.ORDINARY, 2, "2900", "Уютный стандартный номер с видом на сад"),
            room("102", RoomClass.ORDINARY, 2, "3000", "Стандартный номер с раздельными кроватями"),
            room("103", RoomClass.ORDINARY, 3, "3500", "Стандартный тройной номер"),
            room("104", RoomClass.ORDINARY, 2, "3200", "Компактный стандартный номер"),
            // MIDDLE: 6000–9000 руб/ночь, 2–4 гостя; скидка 30% на SAUNA/BATH/POOL
            room("201", RoomClass.MIDDLE,   2, "6000", "Комфортный номер с балконом и видом на парк"),
            room("202", RoomClass.MIDDLE,   3, "7500", "Семейный комфорт с дополнительным диваном"),
            room("203", RoomClass.MIDDLE,   4, "9000", "Просторный комфорт для компании"),
            // PREMIUM: 12000–18000 руб/ночь; POOL бесплатен, первая SAUNA/BATH/MASSAGE = 0 руб
            room("301", RoomClass.PREMIUM,  2, "12000", "Люкс с джакузи и панорамным видом на город"),
            room("302", RoomClass.PREMIUM,  4, "15000", "Двухкомнатный люкс с гостиной"),
            room("303", RoomClass.PREMIUM,  2, "18000", "Пентхаус на верхнем этаже с террасой")
        ));

        log.info("Seeded {} rooms", roomRepository.count());
    }

    // Фабричный метод: избавляет от повторяющегося Room.builder()...build() в seed().
    private Room room(String number, RoomClass roomClass, int capacity, String price, String description) {
        return Room.builder()
                .roomNumber(number)
                .roomClass(roomClass)
                .capacity(capacity)
                // new BigDecimal(String) — точное представление; new BigDecimal(double) даёт погрешности.
                .pricePerNight(new BigDecimal(price))
                .description(description)
                .build();
    }
}
