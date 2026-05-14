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

// Интеграционный тест: поднимает полный Spring Boot контекст + реальный PostgreSQL в Docker.
//
// @SpringBootTest(RANDOM_PORT) — запускает встроенный Tomcat на случайном порту.
//   Позволяет проверить весь стек: HTTP → контроллер → сервис → репозиторий → реальная БД.
// @Testcontainers — JUnit 5 расширение: управляет жизненным циклом @Container полей.
// @TestPropertySource — переопределяет Kafka bootstrap-servers на несуществующий адрес.
//   CustomerService не использует Kafka, но spring.kafka есть в classpath;
//   Spring Kafka соединяется лениво → контекст стартует без реального брокера.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=localhost:9999")
class CustomerControllerIT {

    // PostgreSQLContainer — запускает Docker-контейнер с postgres:16-alpine перед тестами.
    // static — контейнер создаётся один раз на весь класс (не пересоздаётся для каждого теста).
    // @ServiceConnection (Spring Boot 3.1+) — автоматически конфигурирует spring.datasource.*
    //   из адреса контейнера. Не нужно прописывать url/username/password вручную.
    // withInitScript — выполняет SQL-скрипт перед Hibernate: создаёт схему customer.
    //   Это важно: Hibernate делает DDL (create table) только если схема уже существует.
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withInitScript("create-customer-schema.sql");

    // TestRestTemplate — HTTP-клиент для интеграционных тестов; знает порт тестового Tomcat.
    // Не бросает исключение при 4xx/5xx — возвращает ResponseEntity со статусом.
    @Autowired TestRestTemplate restTemplate;

    // ── register ─────────────────────────────────────────────────────────────────

    // Полный happy path: запрос проходит HTTP → контроллер → сервис → PostgreSQL → ответ с JWT.
    @Test
    void register_validRequest_returns200WithJwtToken() {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", validRegisterRequest("it1@example.com"), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank(); // реальный JWT от JwtUtil
        assertThat(response.getBody().getEmail()).isEqualTo("it1@example.com");
        assertThat(response.getBody().getRole()).isEqualTo("CUSTOMER");
    }

    // Проверяем роль через реальную БД: сохранили CUSTOMER, прочитали CUSTOMER.
    @Test
    void register_newCustomer_alwaysReceivesCustomerRole() {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", validRegisterRequest("it2@example.com"), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRole()).isEqualTo("CUSTOMER");
    }

    // Дубликат email: первый запрос — ок, второй — 400.
    // Проверяет UNIQUE constraint в реальной PostgreSQL, а не только логику в сервисе.
    @Test
    void register_duplicateEmail_returns400() {
        // Первая регистрация — успех.
        restTemplate.postForEntity("/api/auth/register",
                validRegisterRequest("dup@example.com"), AuthResponse.class);

        // Вторая с тем же email — должна вернуть 400.
        ResponseEntity<String> second = restTemplate.postForEntity(
                "/api/auth/register", validRegisterRequest("dup@example.com"), String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // Bean Validation сработала на уровне HTTP: до БД запрос не дошёл.
    @Test
    void register_invalidEmail_returns400() {
        RegisterRequest req = validRegisterRequest("not-an-email");
        req.setEmail("not-an-email"); // нарушает @Email

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── login ─────────────────────────────────────────────────────────────────────

    // Сначала регистрируем → потом логинимся с теми же данными → получаем токен.
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

    // BCrypt-проверка в реальной БД: неверный пароль → 400.
    @Test
    void login_wrongPassword_returns400() {
        restTemplate.postForEntity("/api/auth/register",
                validRegisterRequest("login2@example.com"), AuthResponse.class);

        LoginRequest login = new LoginRequest();
        login.setEmail("login2@example.com");
        login.setPassword("wrong-password"); // BCrypt matches → false → IllegalArgumentException

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", login, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // Email не существует в БД → NoSuchElementException → 404.
    @Test
    void login_unknownEmail_returns404() {
        LoginRequest login = new LoginRequest();
        login.setEmail("nobody@example.com");
        login.setPassword("password123");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", login, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // Round-trip: userId в токене регистрации и логина должен совпадать.
    // Проверяет, что JwtUtil корректно кодирует id, полученный от PostgreSQL (SERIAL).
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
        // userId должен быть одинаков — он берётся из БД, не генерируется случайно.
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
