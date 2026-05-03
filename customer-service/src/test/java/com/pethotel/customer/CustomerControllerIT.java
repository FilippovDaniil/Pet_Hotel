package com.pethotel.customer;

import com.pethotel.customer.dto.AuthResponse;
import com.pethotel.customer.dto.LoginRequest;
import com.pethotel.customer.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests: full HTTP stack + real PostgreSQL.
 * spring.kafka is on the classpath but CustomerService never sends messages,
 * so no Kafka broker is required — the template connects lazily.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=localhost:9999")
class CustomerControllerIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withInitScript("create-customer-schema.sql");

    @Autowired TestRestTemplate restTemplate;

    // ── register ─────────────────────────────────────────────────────────────────

    @Test
    void register_validRequest_returns200WithJwtToken() {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", validRegisterRequest("it1@example.com"), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getEmail()).isEqualTo("it1@example.com");
        assertThat(response.getBody().getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    void register_newCustomer_alwaysReceivesCustomerRole() {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", validRegisterRequest("it2@example.com"), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    void register_duplicateEmail_returns400() {
        restTemplate.postForEntity("/api/auth/register",
                validRegisterRequest("dup@example.com"), AuthResponse.class);

        ResponseEntity<String> second = restTemplate.postForEntity(
                "/api/auth/register", validRegisterRequest("dup@example.com"), String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_invalidEmail_returns400() {
        RegisterRequest req = validRegisterRequest("not-an-email");
        req.setEmail("not-an-email");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── login ─────────────────────────────────────────────────────────────────────

    @Test
    void login_afterRegister_returns200WithToken() {
        restTemplate.postForEntity("/api/auth/register",
                validRegisterRequest("login1@example.com"), AuthResponse.class);

        LoginRequest login = new LoginRequest();
        login.setEmail("login1@example.com");
        login.setPassword("password123");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", login, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isNotBlank();
    }

    @Test
    void login_wrongPassword_returns400() {
        restTemplate.postForEntity("/api/auth/register",
                validRegisterRequest("login2@example.com"), AuthResponse.class);

        LoginRequest login = new LoginRequest();
        login.setEmail("login2@example.com");
        login.setPassword("wrong-password");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", login, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_unknownEmail_returns404() {
        LoginRequest login = new LoginRequest();
        login.setEmail("nobody@example.com");
        login.setPassword("password123");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", login, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void registerThenLogin_tokenContainsCorrectUserId() {
        AuthResponse registered = restTemplate.postForEntity(
                "/api/auth/register", validRegisterRequest("roundtrip@example.com"),
                AuthResponse.class).getBody();

        LoginRequest login = new LoginRequest();
        login.setEmail("roundtrip@example.com");
        login.setPassword("password123");

        AuthResponse loggedIn = restTemplate.postForEntity(
                "/api/auth/login", login, AuthResponse.class).getBody();

        assertThat(loggedIn).isNotNull();
        assertThat(registered).isNotNull();
        assertThat(loggedIn.getUserId()).isEqualTo(registered.getUserId());
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private RegisterRequest validRegisterRequest(String email) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword("password123");
        req.setFirstName("Ivan");
        req.setLastName("Ivanov");
        return req;
    }
}
