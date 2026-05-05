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

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        seedUser("customer@hotel.com", "customer123", "Алексей", "Иванов", "+7 999 100-00-01", Role.CUSTOMER);
        seedUser("reception@hotel.com", "reception123", "Мария", "Смирнова", "+7 999 100-00-02", Role.RECEPTION);
        seedUser("admin@hotel.com", "admin123", "Дмитрий", "Козлов", "+7 999 100-00-03", Role.ADMIN);
    }

    private void seedUser(String email, String password, String firstName, String lastName, String phone, Role role) {
        if (customerRepository.existsByEmail(email)) return;
        customerRepository.save(Customer.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .role(role)
                .build());
        log.info("Demo user created: {} ({})", email, role);
    }
}
