package com.pethotel.customer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

// @RestControllerAdvice — перехватывает исключения из всех @RestController в этом сервисе.
// Без этого класса Spring вернул бы стандартный Whitelabel Error Page или 500 с пустым телом.
// Реализует единый формат ошибок: {"error": "..."} для всего customer-service API.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // NoSuchElementException бросается в CustomerService, когда клиент не найден по id или email.
    // → 404 Not Found с понятным сообщением.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        log.warn("Not found: {}", ex.getMessage());
        // Map.of() — неизменяемая карта; Jackson сериализует в {"error": "..."}
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    // IllegalArgumentException бросается при дублировании email (register) или неверном пароле (login).
    // → 400 Bad Request.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    // MethodArgumentNotValidException — Spring бросает при провале @Valid (Bean Validation).
    // Собираем все ошибки валидации в одну строку: "email: must be a well-formed email address".
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                // reduce — склеиваем несколько ошибок через "; "
                .reduce((a, b) -> a + "; " + b).orElse("Validation error");
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    // Catch-all — ловит всё остальное (NPE, ошибки БД и т.д.).
    // log.error — логируем полный стектрейс для диагностики.
    // Клиенту возвращаем только "Internal server error" — внутренние детали не раскрываем.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
    }
}
