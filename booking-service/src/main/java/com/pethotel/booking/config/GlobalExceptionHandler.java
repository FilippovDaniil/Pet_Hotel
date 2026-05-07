package com.pethotel.booking.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

// Централизованная обработка исключений booking-service.
// Особенность по сравнению с другими сервисами: IllegalArgumentException и IllegalStateException
// объединены в один обработчик — оба типа означают "клиент сделал что-то неверное" → 400.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // NoSuchElementException: бронирование или номер не найден → 404.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    // IllegalArgumentException: некорректный запрос (checkIn >= checkOut, нет прав на отмену).
    // IllegalStateException: недопустимый переход статуса (подтвердить CANCELLED бронирование).
    // Оба → 400 Bad Request: семантически верно, клиент передал неверные данные или нарушил бизнес-правило.
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    // @Valid провалился на BookingRequest или AmenityBookingRequest → 400 с перечнем ошибок полей.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("Validation error");
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    // Catch-all: например, WebClient-ошибка при обращении к room-service → 500.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
    }
}
