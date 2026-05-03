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

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        Customer customer = Customer.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(Role.CUSTOMER)
                .build();
        customer = customerRepository.save(customer);
        log.info("Customer registered: id={} email={}", customer.getId(), customer.getEmail());
        String token = jwtUtil.generateToken(customer.getId(), customer.getEmail(), customer.getRole().name());
        return new AuthResponse(token, customer.getId(), customer.getEmail(), customer.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + request.getEmail()));
        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }
        log.info("Customer login: id={} email={}", customer.getId(), customer.getEmail());
        String token = jwtUtil.generateToken(customer.getId(), customer.getEmail(), customer.getRole().name());
        return new AuthResponse(token, customer.getId(), customer.getEmail(), customer.getRole().name());
    }

    public CustomerDto getById(Long id) {
        return toDto(customerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + id)));
    }

    public List<CustomerDto> getAll() {
        return customerRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public CustomerDto updateRole(Long id, Role role) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + id));
        customer.setRole(role);
        log.info("Customer role updated: id={} role={}", id, role);
        return toDto(customerRepository.save(customer));
    }

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
