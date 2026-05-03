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

    @PostMapping
    @Operation(summary = "Create order (CUSTOMER)")
    public ResponseEntity<OrderDto> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(userId, request));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get orders by bookingId")
    public ResponseEntity<List<OrderDto>> getByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(orderService.getByBookingId(bookingId));
    }
}
