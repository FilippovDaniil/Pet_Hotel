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

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks CustomerService customerService;

    // ── register ────────────────────────────────────────────────────────────────

    @Test
    void register_newEmail_returnsTokenAndCustomerData() {
        RegisterRequest req = registerRequest("user@example.com", "password123", "Ivan", "Ivanov");
        when(customerRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        Customer saved = customer(1L, "user@example.com", "hashed", Role.CUSTOMER);
        when(customerRepository.save(any())).thenReturn(saved);
        when(jwtUtil.generateToken(1L, "user@example.com", "CUSTOMER")).thenReturn("jwt-token");

        AuthResponse res = customerService.register(req);

        assertThat(res.getToken()).isEqualTo("jwt-token");
        assertThat(res.getUserId()).isEqualTo(1L);
        assertThat(res.getEmail()).isEqualTo("user@example.com");
        assertThat(res.getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    void register_newCustomer_alwaysGetsCustomerRole() {
        RegisterRequest req = registerRequest("a@b.com", "password123", "A", "B");
        when(customerRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            assertThat(c.getRole()).isEqualTo(Role.CUSTOMER);
            return customer(1L, c.getEmail(), "hashed", Role.CUSTOMER);
        });
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("token");

        customerService.register(req);

        verify(customerRepository).save(argThat(c -> c.getRole() == Role.CUSTOMER));
    }

    @Test
    void register_duplicateEmail_throwsIllegalArgument() {
        RegisterRequest req = registerRequest("existing@example.com", "password123", "A", "B");
        when(customerRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void register_passwordIsHashed_notStoredPlaintext() {
        RegisterRequest req = registerRequest("a@b.com", "secret", "A", "B");
        when(customerRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("$bcrypt$hashed");
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            assertThat(c.getPasswordHash()).isEqualTo("$bcrypt$hashed");
            assertThat(c.getPasswordHash()).doesNotContain("secret");
            return customer(1L, c.getEmail(), c.getPasswordHash(), Role.CUSTOMER);
        });
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("token");

        customerService.register(req);
    }

    // ── login ────────────────────────────────────────────────────────────────────

    @Test
    void login_correctCredentials_returnsToken() {
        LoginRequest req = loginRequest("user@example.com", "password123");
        Customer c = customer(1L, "user@example.com", "hashed", Role.CUSTOMER);
        when(customerRepository.findByEmail("user@example.com")).thenReturn(Optional.of(c));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "user@example.com", "CUSTOMER")).thenReturn("jwt-token");

        AuthResponse res = customerService.login(req);

        assertThat(res.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_wrongPassword_throwsIllegalArgument() {
        LoginRequest req = loginRequest("user@example.com", "wrong");
        Customer c = customer(1L, "user@example.com", "hashed", Role.CUSTOMER);
        when(customerRepository.findByEmail("user@example.com")).thenReturn(Optional.of(c));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> customerService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid password");
    }

    @Test
    void login_unknownEmail_throwsNoSuchElement() {
        LoginRequest req = loginRequest("unknown@example.com", "password123");
        when(customerRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.login(req))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── getById ──────────────────────────────────────────────────────────────────

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

    @Test
    void getById_notFound_throwsNoSuchElement() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ── getAll ───────────────────────────────────────────────────────────────────

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

    @Test
    void getAll_emptyRepo_returnsEmptyList() {
        when(customerRepository.findAll()).thenReturn(List.of());
        assertThat(customerService.getAll()).isEmpty();
    }

    // ── updateRole ───────────────────────────────────────────────────────────────

    @Test
    void updateRole_setsNewRole() {
        Customer c = customer(1L, "u@u.com", "h", Role.CUSTOMER);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(c));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerDto result = customerService.updateRole(1L, Role.ADMIN);

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        verify(customerRepository).save(argThat(saved -> saved.getRole() == Role.ADMIN));
    }

    @Test
    void updateRole_notFound_throwsNoSuchElement() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateRole(99L, Role.ADMIN))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

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

    private Customer customer(Long id, String email, String hash, Role role) {
        return Customer.builder()
                .id(id).email(email).passwordHash(hash)
                .firstName("First").lastName("Last").role(role).build();
    }
}
