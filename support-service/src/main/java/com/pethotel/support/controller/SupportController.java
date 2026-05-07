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

@Slf4j
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "Support", description = "Чат поддержки между клиентом и администратором")
public class SupportController {

    private final SupportService supportService;

    // ── Customer endpoints ────────────────────────────────────────────────────

    @GetMapping("/messages")
    @Operation(summary = "Получить свои сообщения [CUSTOMER]")
    public ResponseEntity<List<SupportMessageDto>> getMyMessages(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(supportService.getCustomerMessages(userId));
    }

    @PostMapping("/messages")
    @Operation(summary = "Отправить сообщение в поддержку [CUSTOMER]")
    public ResponseEntity<SupportMessageDto> sendMessage(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(supportService.sendCustomerMessage(userId, userEmail, request.getContent()));
    }

    @GetMapping("/messages/unread-count")
    @Operation(summary = "Количество непрочитанных ответов от поддержки [CUSTOMER]")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(Map.of("count", supportService.getUnreadCountForCustomer(userId)));
    }

    @PostMapping("/messages/read")
    @Operation(summary = "Отметить ответы поддержки как прочитанные [CUSTOMER]")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader("X-User-Id") Long userId) {
        supportService.markAsReadByCustomer(userId);
        return ResponseEntity.ok().build();
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @GetMapping("/admin/conversations")
    @Operation(summary = "Список всех диалогов [ADMIN]")
    public ResponseEntity<List<ConversationSummaryDto>> getConversations() {
        return ResponseEntity.ok(supportService.getConversationSummaries());
    }

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
