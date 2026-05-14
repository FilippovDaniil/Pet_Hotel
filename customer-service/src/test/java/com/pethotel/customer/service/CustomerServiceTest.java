package com.pethotel.customer.service;

import com.pethotel.common.enums.Role;
import com.pethotel.common.security.JwtUtil;
import com.pethotel.customer.dto.*;
import com.pethotel.customer.entity.Customer;
import com.pethotel.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Unit-тест: изолированная проверка бизнес-логики CustomerService без БД, HTTP и Spring-контекста.
// @ExtendWith(MockitoExtension.class) — JUnit 5 запускает Mockito: создаёт @Mock и инжектирует в @InjectMocks.
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    // @Mock — Mockito создаёт фиктивный объект; все методы возвращают null/0/false по умолчанию.
    // when(...).thenReturn(...) задаёт нужное поведение в каждом тесте.
    @Mock CustomerRepository customerRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    // @InjectMocks — Mockito создаёт реальный CustomerService и инжектирует в него все @Mock-поля.
    // Это заменяет Spring-контекст и делает тест быстрым (< 1 с без запуска БД/Kafka).
    @InjectMocks CustomerService customerService;

    // ── register ────────────────────────────────────────────────────────────────

    // Happy path: новый email → регистрация прошла, вернули токен и данные.
    @Test
    void register_newEmail_returnsTokenAndCustomerData() {
        RegisterRequest req = registerRequest("user@example.com", "password123", "Ivan", "Ivanov");
        // Задаём поведение mock'ов: email не занят, пароль хэшируется, save возвращает объект с id.
        when(customerRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        Customer saved = customer(1L, "user@example.com", "hashed", Role.CUSTOMER);
        when(customerRepository.save(any())).thenReturn(saved);
        when(jwtUtil.generateToken(1L, "user@example.com", "CUSTOMER")).thenReturn("jwt-token");

        AuthResponse res = customerService.register(req);

        // Проверяем все поля AuthResponse.
        assertThat(res.getToken()).isEqualTo("jwt-token");
        assertThat(res.getUserId()).isEqualTo(1L);
        assertThat(res.getEmail()).isEqualTo("user@example.com");
        assertThat(res.getRole()).isEqualTo("CUSTOMER");
    }

    // Новый пользователь всегда получает роль CUSTOMER — нельзя зарегистрироваться как ADMIN.
    @Test
    void register_newCustomer_alwaysGetsCustomerRole() {
        RegisterRequest req = registerRequest("a@b.com", "password123", "A", "B");
        when(customerRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        // thenAnswer — перехватываем аргумент save() и сразу проверяем роль внутри лямбды.
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer c = inv.getArgument(0); // получаем объект, который передан в save()
            assertThat(c.getRole()).isEqualTo(Role.CUSTOMER);
            return customer(1L, c.getEmail(), "hashed", Role.CUSTOMER);
        });
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("token");

        customerService.register(req);

        // Двойная проверка: через argThat убеждаемся, что save был вызван с CUSTOMER-ролью.
        verify(customerRepository).save(argThat(c -> c.getRole() == Role.CUSTOMER));
    }

    // Дублирующий email → IllegalArgumentException (→ 400 в GlobalExceptionHandler).
    @Test
    void register_duplicateEmail_throwsIllegalArgument() {
        RegisterRequest req = registerRequest("existing@example.com", "password123", "A", "B");
        when(customerRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // assertThatThrownBy — AssertJ: проверяем тип и сообщение брошенного исключения.
        assertThatThrownBy(() -> customerService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    // Убеждаемся, что пароль хэшируется — rawPassword никогда не сохраняется в Entity.
    @Test
    void register_passwordIsHashed_notStoredPlaintext() {
        RegisterRequest req = registerRequest("a@b.com", "secret", "A", "B");
        when(customerRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("$bcrypt$hashed");
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            // Проверяем, что passwordHash == bcrypt-значение, а не исходный пароль.
            assertThat(c.getPasswordHash()).isEqualTo("$bcrypt$hashed");
            assertThat(c.getPasswordHash()).doesNotContain("secret");
            return customer(1L, c.getEmail(), c.getPasswordHash(), Role.CUSTOMER);
        });
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("token");

        customerService.register(req);
    }

    // ── login ────────────────────────────────────────────────────────────────────

    // Happy path: email найден, пароль совпадает → токен выдан.
    @Test
    void login_correctCredentials_returnsToken() {
        LoginRequest req = loginRequest("user@example.com", "password123");
        Customer c = customer(1L, "user@example.com", "hashed", Role.CUSTOMER);
        when(customerRepository.findByEmail("user@example.com")).thenReturn(Optional.of(c));
        // matches(rawPassword, storedHash) — BCrypt-проверка; здесь mock возвращает true.
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "user@example.com", "CUSTOMER")).thenReturn("jwt-token");

        AuthResponse res = customerService.login(req);

        assertThat(res.getToken()).isEqualTo("jwt-token");
    }

    // Неверный пароль → IllegalArgumentException (→ 400).
    @Test
    void login_wrongPassword_throwsIllegalArgument() {
        LoginRequest req = loginRequest("user@example.com", "wrong");
        Customer c = customer(1L, "user@example.com", "hashed", Role.CUSTOMER);
        when(customerRepository.findByEmail("user@example.com")).thenReturn(Optional.of(c));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false); // пароль не совпал

        assertThatThrownBy(() -> customerService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid password");
    }

    // Неизвестный email → NoSuchElementException (→ 404).
    @Test
    void login_unknownEmail_throwsNoSuchElement() {
        LoginRequest req = loginRequest("unknown@example.com", "password123");
        // Optional.empty() — клиент не найден в БД.
        when(customerRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.login(req))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── getById ──────────────────────────────────────────────────────────────────

    // Клиент найден → DTO содержит все поля (кроме passwordHash).
    @Test
    void getById_existingId_returnsDto() {
        Customer c = customer(5L, "c@example.com", "hash", Role.RECEPTION);
        c.setFirstName("Anna");
        c.setLastName("Petrova");
        c.setPhone("+7000");
        when(customerRepository.findById(5L)).thenReturn(Optional.of(c));

        CustomerDto dto = customerService.getById(5L);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getEmail()).isEqualTo("c@example.com");
        assertThat(dto.getRole()).isEqualTo(Role.RECEPTION);
        assertThat(dto.getFirstName()).isEqualTo("Anna");
    }

    // Несуществующий id → NoSuchElementException с id в сообщении.
    @Test
    void getById_notFound_throwsNoSuchElement() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99"); // сообщение должно упоминать id
    }

    // ── getAll ───────────────────────────────────────────────────────────────────

    // Список клиентов маппится в DTO корректно (email, роль сохраняются).
    @Test
    void getAll_returnsMappedList() {
        Customer c1 = customer(1L, "a@b.com", "h", Role.CUSTOMER);
        Customer c2 = customer(2L, "c@d.com", "h", Role.ADMIN);
        when(customerRepository.findAll()).thenReturn(List.of(c1, c2));

        List<CustomerDto> result = customerService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("a@b.com");
        assertThat(result.get(1).getRole()).isEqualTo(Role.ADMIN);
    }

    // Пустая таблица → пустой список (не null, не исключение).
    @Test
    void getAll_emptyRepo_returnsEmptyList() {
        when(customerRepository.findAll()).thenReturn(List.of());
        assertThat(customerService.getAll()).isEmpty();
    }

    // ── updateRole ───────────────────────────────────────────────────────────────

    // Смена роли: save() вызван с новой ролью, DTO возвращён с ней же.
    @Test
    void updateRole_setsNewRole() {
        Customer c = customer(1L, "u@u.com", "h", Role.CUSTOMER);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(c));
        // thenAnswer с inv.getArgument(0): save возвращает тот же объект, который ему передали.
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerDto result = customerService.updateRole(1L, Role.ADMIN);

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        // verify + argThat: убеждаемся, что в БД был сохранён объект именно с ролью ADMIN.
        verify(customerRepository).save(argThat(saved -> saved.getRole() == Role.ADMIN));
    }

    // Попытка сменить роль несуществующему клиенту → исключение.
    @Test
    void updateRole_notFound_throwsNoSuchElement() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateRole(99L, Role.ADMIN))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────
    // Вспомогательные фабричные методы для построения тестовых объектов.
    // Выделены, чтобы не дублировать boilerplate в каждом тесте.

    private RegisterRequest registerRequest(String email, String password, String first, String last) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFirstName(first);
        req.setLastName(last);
        return req;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    // @Builder в Customer позволяет задавать только нужные поля; firstName/lastName имеют заглушки.
    private Customer customer(Long id, String email, String hash, Role role) {
        return Customer.builder()
                .id(id).email(email).passwordHash(hash)
                .firstName("First").lastName("Last").role(role).build();
    }
}
