package com.pethotel.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pethotel.customer.config.GlobalExceptionHandler;
import com.pethotel.customer.config.SecurityConfig;
import com.pethotel.customer.dto.AuthResponse;
import com.pethotel.customer.dto.LoginRequest;
import com.pethotel.customer.dto.RegisterRequest;
import com.pethotel.customer.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Web-slice тест: поднимает только слой контроллера (без БД и Kafka).
// @WebMvcTest(AuthController.class) — Spring поднимает MockMvc только для AuthController.
// @Import(...) — явно подключаем SecurityConfig (permitAll) и GlobalExceptionHandler (нужен для 400/404).
//   Без @Import(GlobalExceptionHandler.class) тест не увидел бы кастомный формат ошибок.
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    // MockMvc — позволяет отправлять HTTP-запросы в тест без реального TCP-соединения.
    @Autowired MockMvc mockMvc;
    // ObjectMapper — сериализует тестовые объекты в JSON для тела запроса.
    @Autowired ObjectMapper objectMapper;
    // @MockBean — заменяет реальный CustomerService заглушкой; регистрируется в Spring-контексте теста.
    @MockBean CustomerService customerService;

    // ── register ────────────────────────────────────────────────────────────────

    // Happy path: сервис вернул AuthResponse → контроллер сериализует его в JSON с кодом 200.
    @Test
    void register_validRequest_returns200WithToken() throws Exception {
        when(customerService.register(any()))
                .thenReturn(new AuthResponse("jwt-token", 1L, "user@example.com", "CUSTOMER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        // writeValueAsString — сериализует объект в JSON-строку для тела запроса.
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                // andExpect — цепочка проверок: статус HTTP и поля JSON-ответа.
                .andExpect(status().isOk())
                // jsonPath("$.token") — JSONPath-выражение: $ = корень, .token = поле token.
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    // Bean Validation: невалидный email → 400 без вызова сервиса.
    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setEmail("not-an-email"); // нарушает @Email

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()); // @Valid + MethodArgumentNotValidException → 400
    }

    // Bean Validation: пароль короче 6 символов → 400.
    @Test
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setPassword("abc"); // нарушает @Size(min = 6)

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // Bean Validation: пустое имя → 400.
    @Test
    void register_blankFirstName_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setFirstName(""); // нарушает @NotBlank

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // Bean Validation: фамилия из пробелов → 400 (@NotBlank проверяет trim).
    @Test
    void register_blankLastName_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setLastName("   "); // пробелы — тоже @NotBlank violation

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // Сервис бросил IllegalArgumentException → GlobalExceptionHandler вернул 400 с полем "error".
    @Test
    void register_duplicateEmail_returns400WithErrorField() throws Exception {
        when(customerService.register(any()))
                .thenThrow(new IllegalArgumentException("Email already registered: user@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                .andExpect(status().isBadRequest())
                // jsonPath("$.error").exists() — проверяем только наличие поля, не его значение.
                .andExpect(jsonPath("$.error").exists());
    }

    // ── login ────────────────────────────────────────────────────────────────────

    // Happy path: сервис вернул токен → 200 с телом.
    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        when(customerService.login(any()))
                .thenReturn(new AuthResponse("jwt-token", 1L, "user@example.com", "CUSTOMER"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    // Неверный пароль: сервис бросил IllegalArgumentException → GlobalExceptionHandler → 400.
    @Test
    void login_wrongPassword_returns400() throws Exception {
        when(customerService.login(any()))
                .thenThrow(new IllegalArgumentException("Invalid password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid password")); // точное сообщение
    }

    // Неизвестный email: сервис бросил NoSuchElementException → GlobalExceptionHandler → 404.
    @Test
    void login_unknownEmail_returns404() throws Exception {
        when(customerService.login(any()))
                .thenThrow(new NoSuchElementException("Customer not found: unknown@example.com"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest())))
                .andExpect(status().isNotFound());
    }

    // Bean Validation: пустой email в login → 400 до вызова сервиса.
    @Test
    void login_blankEmail_returns400() throws Exception {
        LoginRequest req = validLoginRequest();
        req.setEmail(""); // нарушает @NotBlank + @Email

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private RegisterRequest validRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@example.com");
        req.setPassword("password123");
        req.setFirstName("Ivan");
        req.setLastName("Ivanov");
        return req;
    }

    private LoginRequest validLoginRequest() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("password123");
        return req;
    }
}
