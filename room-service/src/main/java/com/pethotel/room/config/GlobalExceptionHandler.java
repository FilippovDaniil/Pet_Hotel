package com.pethotel.room.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

// Централизованная обработка исключений для room-service.
// Все обработчики возвращают {"error": "..."} — единый формат ошибок API.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // NoSuchElementException из findRoom() → 404 Not Found.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    // IllegalArgumentException (например, попытка создать номер с дублирующимся номером) → 400.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    // MethodArgumentNotValidException — провал @Valid на RoomRequest/@RoomAvailabilityRequest.
    // Собираем все ошибки полей в одну строку: "pricePerNight: must be greater than 0.01".
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("Validation error");
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    // Catch-all: логируем полный стектрейс для диагностики, клиенту — только "Internal server error".
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
    }
}
