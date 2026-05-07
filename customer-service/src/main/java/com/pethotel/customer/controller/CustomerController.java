package com.pethotel.customer.controller;

import com.pethotel.common.enums.Role;
import com.pethotel.customer.dto.CustomerDto;
import com.pethotel.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Контроллер для работы с профилями клиентов.
// Все эндпоинты защищены JWT на уровне Gateway — сюда не доберётся неаутентифицированный запрос.
// Авторизация по роли (ADMIN-only) в этом учебном проекте не реализована в коде сервиса
// и предполагается как будущее улучшение (через @PreAuthorize или проверку X-User-Role).
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService customerService;

    // GET /api/customers/me — профиль текущего пользователя.
    // @RequestHeader("X-User-Id") — заголовок добавлен Gateway после валидации JWT.
    // Сервис доверяет этому заголовку и не перепроверяет токен.
    @GetMapping("/me")
    @Operation(summary = "Get current customer profile")
    public ResponseEntity<CustomerDto> getMe(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(customerService.getById(userId));
    }

    // GET /api/customers/{id} — профиль по ID; используется ADMIN и RECEPTION.
    // @PathVariable — извлекает {id} из URL: /api/customers/42 → id = 42.
    @GetMapping("/{id}")
    @Operation(summary = "Get customer by id (ADMIN, RECEPTION)")
    public ResponseEntity<CustomerDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    // GET /api/customers — все клиенты; только ADMIN (роль не проверяется в коде — упрощение).
    @GetMapping
    @Operation(summary = "Get all customers (ADMIN)")
    public ResponseEntity<List<CustomerDto>> getAll() {
        return ResponseEntity.ok(customerService.getAll());
    }

    // PUT /api/customers/{id}/role?role=ADMIN — назначить роль клиенту.
    // @RequestParam — читает параметр из query string: ?role=RECEPTION.
    // Role — enum; Spring конвертирует строку "RECEPTION" в Role.RECEPTION автоматически
    //         через ConversionService (есть для всех enum по умолчанию).
    @PutMapping("/{id}/role")
    @Operation(summary = "Update customer role (ADMIN)")
    public ResponseEntity<CustomerDto> updateRole(@PathVariable Long id,
                                                   @RequestParam Role role) {
        return ResponseEntity.ok(customerService.updateRole(id, role));
    }
}
