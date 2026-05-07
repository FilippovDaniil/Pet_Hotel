package com.pethotel.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// DTO для запроса аутентификации (POST /api/auth/login).
// Содержит только email и пароль — минимально необходимые данные.
// @Valid в AuthController запускает аннотации Bean Validation перед вызовом сервиса.
@Data
public class LoginRequest {

    @Email @NotBlank
    private String email;

    @NotBlank
    private String password;
}
