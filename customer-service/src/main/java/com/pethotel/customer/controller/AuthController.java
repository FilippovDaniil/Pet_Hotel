package com.pethotel.customer.controller;

import com.pethotel.customer.dto.AuthResponse;
import com.pethotel.customer.dto.LoginRequest;
import com.pethotel.customer.dto.RegisterRequest;
import com.pethotel.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// @RestController = @Controller + @ResponseBody.
//   Каждый возвращаемый объект сериализуется Jackson в JSON-тело ответа.
// @RequestMapping("/api/auth") — базовый путь; в Gateway этот путь объявлен публичным
//   (не требует JWT), поскольку именно здесь клиент получает токен.
// @Tag — метаданные для Swagger UI (группировка эндпоинтов).
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final CustomerService customerService;

    // @PostMapping("/register") → POST /api/auth/register
    // @Valid — запускает Bean Validation на RegisterRequest перед передачей в сервис.
    //          Если @Email/@NotBlank/@Size не выполнены → MethodArgumentNotValidException → 400.
    // @RequestBody — десериализует JSON-тело запроса в объект RegisterRequest.
    @PostMapping("/register")
    @Operation(summary = "Register a new customer")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // ResponseEntity.ok() — 200 OK с телом ответа.
        return ResponseEntity.ok(customerService.register(request));
    }

    // POST /api/auth/login — проверяет пароль и возвращает JWT-токен.
    // Публичный эндпоинт: Gateway не требует токен для пути /api/auth/**.
    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(customerService.login(request));
    }
}
