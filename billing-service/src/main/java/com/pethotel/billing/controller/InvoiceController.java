package com.pethotel.billing.controller;

import com.pethotel.billing.dto.InvoiceDto;
import com.pethotel.billing.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices")
public class InvoiceController {

    private final BillingService billingService;

    @GetMapping("/my")
    @Operation(summary = "Get my invoices (uses X-User-Id header)")
    public ResponseEntity<List<InvoiceDto>> getMyInvoices(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(billingService.getByCustomerId(userId));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get invoice by bookingId")
    public ResponseEntity<InvoiceDto> getByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(billingService.getByBookingId(bookingId));
    }

    @PostMapping("/{bookingId}/pay")
    @Operation(summary = "Pay invoice for booking (RECEPTION)")
    public ResponseEntity<InvoiceDto> pay(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(billingService.pay(bookingId));
    }
}
