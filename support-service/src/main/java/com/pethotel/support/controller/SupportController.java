package com.pethotel.support.controller;

import com.pethotel.support.dto.ConversationSummaryDto;
import com.pethotel.support.dto.SendMessageRequest;
import com.pethotel.support.dto.SupportMessageDto;
import com.pethotel.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// @RestController = @Controller + @ResponseBody.
//   @Controller — маркирует класс как обработчик HTTP-запросов.
//   @ResponseBody — каждый возвращаемый объект сериализуется в JSON (через Jackson), а не рендерится как View.
// @RequestMapping("/api/support") — базовый путь для всех методов этого контроллера.
// @Tag — метаданные для Swagger UI (группировка эндпоинтов под одним именем).
@Slf4j
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "Support", description = "Чат поддержки между клиентом и администратором")
public class SupportController {

    private final SupportService supportService;

    // ── Customer endpoints ───────────────────────────────────────────────────

    // @GetMapping("/messages") — обрабатывает GET /api/support/messages.
    // @Operation — описание для Swagger UI (документация в браузере).
    @GetMapping("/messages")
    @Operation(summary = "Получить свои сообщения [CUSTOMER]")
    public ResponseEntity<List<SupportMessageDto>> getMyMessages(
            // @RequestHeader — читает значение HTTP-заголовка.
            // API Gateway добавляет X-User-Id из JWT-токена перед проксированием запроса.
            // Сервис доверяет этому заголовку — проверка JWT уже выполнена на Gateway.
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(supportService.getCustomerMessages(userId));
    }

    @PostMapping("/messages")
    @Operation(summary = "Отправить сообщение в поддержку [CUSTOMER]")
    public ResponseEntity<SupportMessageDto> sendMessage(
            @RequestHeader("X-User-Id") Long userId,
            // X-User-Email — новый заголовок, добавлен в JwtAuthFilter этой сессии.
            // Нужен для сохранения email в сообщении (чтобы admin видел, кто написал).
            @RequestHeader("X-User-Email") String userEmail,
            // @Valid — запускает Bean Validation (@NotBlank, @Size) на теле запроса.
            // @RequestBody — десериализует JSON из тела запроса в объект SendMessageRequest.
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(
                supportService.sendCustomerMessage(userId, userEmail, request.getContent()));
    }

    @GetMapping("/messages/unread-count")
    @Operation(summary = "Количество непрочитанных ответов от поддержки [CUSTOMER]")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("X-User-Id") Long userId) {
        // Возвращаем Map вместо примитива, чтобы JSON был {"count": 3}, а не просто 3.
        // Фронтенд может добавить другие поля в будущем без изменения контракта.
        return ResponseEntity.ok(Map.of("count", supportService.getUnreadCountForCustomer(userId)));
    }

    @PostMapping("/messages/read")
    @Operation(summary = "Отметить ответы поддержки как прочитанные [CUSTOMER]")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader("X-User-Id") Long userId) {
        supportService.markAsReadByCustomer(userId);
        // ResponseEntity.ok().build() — 200 OK с пустым телом (Void).
        // Пустое тело логично: клиент уже знает, что сделал, — ничего возвращать не нужно.
        return ResponseEntity.ok().build();
    }

    // ── Admin endpoints ──────────────────────────────────────────────────────

    // Роль проверяется неявно — API Gateway пропускает запрос, только если токен валидный.
    // Проверку роли (ADMIN) здесь не делаем: в учебном проекте это упрощение.
    // В production нужно либо проверять X-User-Role, либо использовать @PreAuthorize.

    @GetMapping("/admin/conversations")
    @Operation(summary = "Список всех диалогов [ADMIN]")
    public ResponseEntity<List<ConversationSummaryDto>> getConversations() {
        return ResponseEntity.ok(supportService.getConversationSummaries());
    }

    // @PathVariable — извлекает {customerId} из URL-шаблона.
    // Например, GET /api/support/admin/conversations/42 → customerId = 42.
    @GetMapping("/admin/conversations/{customerId}")
    @Operation(summary = "Получить диалог с конкретным клиентом [ADMIN]")
    public ResponseEntity<List<SupportMessageDto>> getConversation(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(supportService.getAdminConversation(customerId));
    }

    @PostMapping("/admin/conversations/{customerId}/messages")
    @Operation(summary = "Ответить клиенту [ADMIN]")
    public ResponseEntity<SupportMessageDto> replyToCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(supportService.sendAdminMessage(customerId, request.getContent()));
    }

    @PostMapping("/admin/conversations/{customerId}/read")
    @Operation(summary = "Отметить сообщения клиента как прочитанные [ADMIN]")
    public ResponseEntity<Void> markConversationAsRead(
            @PathVariable Long customerId) {
        supportService.markAsReadByAdmin(customerId);
        return ResponseEntity.ok().build();
    }
}
