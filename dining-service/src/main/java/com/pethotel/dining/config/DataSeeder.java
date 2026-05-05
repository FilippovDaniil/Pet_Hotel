package com.pethotel.dining.config;

import com.pethotel.dining.entity.MenuItem;
import com.pethotel.dining.repository.MenuItemRepository;
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

    private final MenuItemRepository menuItemRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (menuItemRepository.count() > 0) return;

        menuItemRepository.saveAll(List.of(
            // Завтраки
            item("Каша овсяная с ягодами",        "250",  "Завтрак"),
            item("Яичница с беконом",              "350",  "Завтрак"),
            item("Омлет с сыром и зеленью",        "320",  "Завтрак"),
            item("Вафли с кленовым сиропом",       "300",  "Завтрак"),
            item("Круассан с маслом и джемом",     "180",  "Завтрак"),
            item("Гранола с йогуртом и фруктами",  "270",  "Завтрак"),

            // Обед
            item("Борщ домашний со сметаной",      "350",  "Обед"),
            item("Крем-суп из тыквы",              "320",  "Обед"),
            item("Цезарь с курицей",               "520",  "Обед"),
            item("Паста карбонара",                "580",  "Обед"),
            item("Котлета с картофельным пюре",    "450",  "Обед"),
            item("Греческий салат",                "380",  "Обед"),

            // Ужин
            item("Стейк рибай (300г)",             "1500", "Ужин"),
            item("Филе лосося на гриле",            "1200", "Ужин"),
            item("Утиная грудка с соусом",         "1100", "Ужин"),
            item("Паста с морепродуктами",         "850",  "Ужин"),

            // Напитки
            item("Кофе американо",                 "180",  "Напитки"),
            item("Капучино",                       "220",  "Напитки"),
            item("Свежевыжатый апельсиновый сок",  "280",  "Напитки"),
            item("Чай чёрный / зелёный",           "150",  "Напитки"),
            item("Минеральная вода (0.5 л)",       "120",  "Напитки"),
            item("Домашний лимонад",               "250",  "Напитки"),

            // Десерты
            item("Чизкейк Нью-Йорк",              "380",  "Десерты"),
            item("Шоколадный фондан",              "420",  "Десерты"),
            item("Тирамису",                       "390",  "Десерты"),
            item("Мороженое (2 шарика)",           "250",  "Десерты")
        ));

        log.info("Seeded {} menu items", menuItemRepository.count());
    }

    private MenuItem item(String name, String price, String category) {
        return MenuItem.builder()
                .name(name)
                .price(new BigDecimal(price))
                .category(category)
                .available(true)
                .build();
    }
}
