package com.pethotel.support.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

// @Slf4j — Lombok генерирует поле: private static final Logger log = LoggerFactory.getLogger(...)
// @RestControllerAdvice — перехватывает исключения из всех @RestController в этом сервисе.
// Без этого класса Spring вернул бы стандартный Whitelabel Error Page или пустой 500.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @ExceptionHandler(X.class) — перехватывает исключения типа X, брошенные в контроллерах и сервисах.
    // NoSuchElementException бросается в SupportService, когда диалог с клиентом не найден.
    // Возвращаем 404 с понятным сообщением вместо 500.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        // Map.of() — создаёт неизменяемую карту; Jackson сериализует её в {"error": "..."}
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    // IllegalArgumentException — бросается при невалидных аргументах бизнес-логики.
    // Пример: передан несуществующий customerId в admin-запросе.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    // IllegalStateException — невозможное состояние системы.
    // Оба (IllegalArgument и IllegalState) → 400 Bad Request.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    // MethodArgumentNotValidException — бросается Spring, когда @Valid аннотация на @RequestBody
    // не проходит проверку (например, @NotBlank не выполнен).
    // Собираем все ошибки в одну строку: "content: Сообщение не может быть пустым"
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation error");
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    // Catch-all — ловит всё остальное (NPE, DB errors и т.д.).
    // log.error — логируем полный stacktrace (это важно для диагностики).
    // Клиенту отдаём только "Internal server error" — детали ошибки не раскрываем наружу.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
    }
}
