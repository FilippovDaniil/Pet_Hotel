package com.pethotel.customer.config;

import com.pethotel.common.enums.Role;
import com.pethotel.customer.entity.Customer;
import com.pethotel.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

// Заполняет БД тестовыми пользователями при первом запуске.
// Необходим, потому что в проекте нет UI для создания ADMIN-пользователя —
// без сидера пришлось бы вручную делать INSERT в БД после каждого docker volume prune.
//
// @Component — Spring регистрирует класс как бин и управляет его жизненным циклом.
// @RequiredArgsConstructor — конструкторная инжекция: repository + passwordEncoder.
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final CustomerRepository customerRepository;
    // PasswordEncoder нужен для хэширования паролей тестовых пользователей.
    private final PasswordEncoder passwordEncoder;

    // @EventListener(ApplicationReadyEvent.class) — метод выполняется ПОСЛЕ того, как Spring-контекст
    // полностью поднялся и все бины инициализированы (в том числе Hibernate создал таблицы).
    // Это безопаснее, чем @PostConstruct — там контекст может быть ещё не готов.
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        // Создаём трёх пользователей: по одному на каждую роль системы.
        seedUser("customer@hotel.com", "customer123", "Алексей",  "Иванов",  "+7 999 100-00-01", Role.CUSTOMER);
        seedUser("reception@hotel.com", "reception123", "Мария",   "Смирнова", "+7 999 100-00-02", Role.RECEPTION);
        seedUser("admin@hotel.com",    "admin123",    "Дмитрий", "Козлов",  "+7 999 100-00-03", Role.ADMIN);
    }

    // Идемпотентный метод: existsByEmail() → не создаём дубликат при повторном запуске.
    // Данные сохраняются в Docker volume → docker-compose down (без -v) их не удаляет.
    private void seedUser(String email, String password, String firstName, String lastName, String phone, Role role) {
        // Если пользователь уже существует — ничего не делаем.
        if (customerRepository.existsByEmail(email)) return;
        customerRepository.save(Customer.builder()
                .email(email)
                // Хэшируем пароль так же, как при обычной регистрации.
                // Сырые пароли нигде не сохраняются — только BCrypt-хэш.
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .role(role)
                .build());
        log.info("Demo user created: {} ({})", email, role);
    }
}
