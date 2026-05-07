package com.pethotel.customer.service;

import com.pethotel.common.enums.Role;
import com.pethotel.common.security.JwtUtil;
import com.pethotel.customer.dto.*;
import com.pethotel.customer.entity.Customer;
import com.pethotel.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

// @Slf4j — Lombok инжектирует логгер: private static final Logger log = LoggerFactory.getLogger(...)
// @Service — маркер слоя бизнес-логики; Spring создаёт Singleton и регистрирует в контексте.
// @RequiredArgsConstructor — конструктор для всех final-полей; Spring инжектирует зависимости.
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    // PasswordEncoder — интерфейс Spring Security; реализация BCryptPasswordEncoder задана в SecurityConfig.
    // Инъекция через интерфейс, а не класс — можно поменять алгоритм без изменения этого кода.
    private final PasswordEncoder passwordEncoder;
    // JwtUtil — не @Component, зарегистрирован как @Bean в AppConfig.
    // Spring находит его в контексте и инжектирует сюда так же, как любой другой бин.
    private final JwtUtil jwtUtil;

    // @Transactional — открывает транзакцию: save() + проверка существования выполняются атомарно.
    // Если что-то упадёт после existsByEmail(), но до save() — изменений в БД не будет.
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Проверяем уникальность email до INSERT, чтобы вернуть понятное 400, а не 500 от БД.
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        Customer customer = Customer.builder()
                .email(request.getEmail())
                // encode() — однонаправленный bcrypt-хэш; открытый пароль нигде не сохраняется.
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                // Новый пользователь всегда получает роль CUSTOMER.
                // Для RECEPTION/ADMIN нужно вручную изменить роль через updateRole (ADMIN-эндпоинт).
                .role(Role.CUSTOMER)
                .build();
        // save() выполняет INSERT; Hibernate возвращает объект с заполненным id из БД (SERIAL).
        customer = customerRepository.save(customer);
        log.info("Customer registered: id={} email={}", customer.getId(), customer.getEmail());
        // Сразу выдаём токен — пользователь авторизован с момента регистрации (нет "подтвердите email").
        String token = jwtUtil.generateToken(customer.getId(), customer.getEmail(), customer.getRole().name());
        return new AuthResponse(token, customer.getId(), customer.getEmail(), customer.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        // Optional.orElseThrow — бросаем NoSuchElementException → GlobalExceptionHandler → 404.
        // Специально не говорим "неверный пароль" vs "не найден email" — это защита от перебора.
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + request.getEmail()));
        // matches(rawPassword, storedHash) — BCrypt заново хэширует raw-пароль с той же солью
        // и сравнивает результат с сохранённым хэшем. Открытый пароль не хранится нигде.
        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }
        log.info("Customer login: id={} email={}", customer.getId(), customer.getEmail());
        String token = jwtUtil.generateToken(customer.getId(), customer.getEmail(), customer.getRole().name());
        return new AuthResponse(token, customer.getId(), customer.getEmail(), customer.getRole().name());
    }

    // Используется в GET /api/customers/me (свой профиль) и GET /api/customers/{id} (для ADMIN/RECEPTION).
    public CustomerDto getById(Long id) {
        return toDto(customerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + id)));
    }

    // Полный список клиентов — только для ADMIN (роль проверяется на уровне gateway).
    public List<CustomerDto> getAll() {
        // findAll() — SELECT * FROM customer.customers; без пагинации (учебный проект).
        return customerRepository.findAll().stream().map(this::toDto).toList();
    }

    // Изменение роли — например, назначение нового сотрудника ресепшн.
    // В реальном проекте здесь нужна дополнительная проверка: только ADMIN может это делать.
    @Transactional
    public CustomerDto updateRole(Long id, Role role) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + id));
        customer.setRole(role);
        log.info("Customer role updated: id={} role={}", id, role);
        // save() на уже существующем объекте (id != null) → UPDATE, не INSERT.
        return toDto(customerRepository.save(customer));
    }

    // Маппинг Entity → DTO: passwordHash не копируется → не попадёт в HTTP-ответ.
    // Явный маппинг (а не MapStruct) — нагляднее для учебного проекта.
    private CustomerDto toDto(Customer c) {
        CustomerDto dto = new CustomerDto();
        dto.setId(c.getId());
        dto.setEmail(c.getEmail());
        dto.setFirstName(c.getFirstName());
        dto.setLastName(c.getLastName());
        dto.setPhone(c.getPhone());
        dto.setRole(c.getRole());
        return dto;
    }
}
