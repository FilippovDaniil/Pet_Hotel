package com.pethotel.dining.controller;

import com.pethotel.dining.dto.OrderDto;
import com.pethotel.dining.dto.OrderRequest;
import com.pethotel.dining.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    // POST /api/orders — создать заказ из буфета.
    // X-User-Id из JWT (добавлен Gateway) — ID клиента, делающего заказ.
    // 201 Created: создан новый ресурс (заказ).
    @PostMapping
    @Operation(summary = "Create order (CUSTOMER)")
    public ResponseEntity<OrderDto> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(userId, request));
    }

    // GET /api/orders/booking/{bookingId} — все заказы по конкретному бронированию.
    // Используется в карточке бронирования (BookingDetailPage на фронте).
    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get orders by bookingId")
    public ResponseEntity<List<OrderDto>> getByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(orderService.getByBookingId(bookingId));
    }

    // GET /api/orders/my — история заказов текущего клиента (сортировка: новые первыми).
    @GetMapping("/my")
    @Operation(summary = "Get my orders (CUSTOMER)")
    public ResponseEntity<List<OrderDto>> getMyOrders(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(orderService.getByCustomerId(userId));
    }
}
