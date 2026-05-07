package com.pethotel.billing.controller;

import com.pethotel.billing.dto.InvoiceDto;
import com.pethotel.billing.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// REST API счетов. Авторизация по роли — на стороне фронтенда и gateway;
// сам контроллер не проверяет роль, но использует X-User-Id для фильтрации.
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices")
public class InvoiceController {

    private final BillingService billingService;

    // GET /api/invoices/my — счета текущего клиента.
    // X-User-Id инжектируется gateway из JWT — клиент не может подставить чужой ID.
    @GetMapping("/my")
    @Operation(summary = "Get my invoices (uses X-User-Id header)")
    public ResponseEntity<List<InvoiceDto>> getMyInvoices(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(billingService.getByCustomerId(userId));
    }

    // GET /api/invoices/booking/{bookingId} — для страницы деталей бронирования.
    // Используется всеми ролями (CUSTOMER, RECEPTION, ADMIN) при просмотре бронирования.
    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get invoice by bookingId")
    public ResponseEntity<InvoiceDto> getByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(billingService.getByBookingId(bookingId));
    }

    // POST /api/invoices/{bookingId}/pay — оплата при выезде клиента (вызывает RECEPTION).
    // userId принимается но не используется в логике — сохранён для будущего аудита.
    @PostMapping("/{bookingId}/pay")
    @Operation(summary = "Pay invoice for booking (RECEPTION)")
    public ResponseEntity<InvoiceDto> pay(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(billingService.pay(bookingId));
    }
}
