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

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean CustomerService customerService;

    // ── register ────────────────────────────────────────────────────────────────

    @Test
    void register_validRequest_returns200WithToken() throws Exception {
        when(customerService.register(any()))
                .thenReturn(new AuthResponse("jwt-token", 1L, "user@example.com", "CUSTOMER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setEmail("not-an-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setPassword("abc");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankFirstName_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setFirstName("");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankLastName_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setLastName("   ");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateEmail_returns400WithErrorField() throws Exception {
        when(customerService.register(any()))
                .thenThrow(new IllegalArgumentException("Email already registered: user@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── login ────────────────────────────────────────────────────────────────────

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

    @Test
    void login_wrongPassword_returns400() throws Exception {
        when(customerService.login(any()))
                .thenThrow(new IllegalArgumentException("Invalid password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid password"));
    }

    @Test
    void login_unknownEmail_returns404() throws Exception {
        when(customerService.login(any()))
                .thenThrow(new NoSuchElementException("Customer not found: unknown@example.com"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void login_blankEmail_returns400() throws Exception {
        LoginRequest req = validLoginRequest();
        req.setEmail("");

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
