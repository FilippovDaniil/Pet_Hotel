package com.pethotel.booking.controller;

import com.pethotel.booking.dto.BookingDto;
import com.pethotel.booking.dto.BookingRequest;
import com.pethotel.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings")
public class BookingController {

    private final BookingService bookingService;

    // POST /api/bookings — создать бронирование.
    // X-User-Id — ID клиента из JWT (добавлен Gateway); клиент не может подделать своё ID.
    // 201 Created — семантически верно: создан новый ресурс.
    @PostMapping
    @Operation(summary = "Create booking (CUSTOMER)")
    public ResponseEntity<BookingDto> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(userId, request));
    }

    // GET /api/bookings/my — бронирования текущего клиента.
    @GetMapping("/my")
    @Operation(summary = "Get my bookings (CUSTOMER)")
    public ResponseEntity<List<BookingDto>> getMyBookings(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(bookingService.getMyBookings(userId));
    }

    // GET /api/bookings/all — все бронирования (RECEPTION/ADMIN).
    @GetMapping("/all")
    @Operation(summary = "Get all bookings (RECEPTION, ADMIN)")
    public ResponseEntity<List<BookingDto>> getAll() {
        return ResponseEntity.ok(bookingService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by id")
    public ResponseEntity<BookingDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    // POST /api/bookings/{id}/cancel — отмена бронирования.
    // X-User-Role с defaultValue = "CUSTOMER": если заголовок отсутствует — считаем клиентом.
    //   Это защита от потенциальных внутренних вызовов без заголовка.
    // isReception = true если роль RECEPTION или ADMIN → без штрафа, без проверки владения.
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking")
    public ResponseEntity<BookingDto> cancel(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "CUSTOMER") String role) {
        boolean isReception = "RECEPTION".equals(role) || "ADMIN".equals(role);
        return ResponseEntity.ok(bookingService.cancel(id, userId, isReception));
    }

    // Переходы статусов: confirm, checkin, checkout — только ресепшн (проверка роли на Gateway).
    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm booking (RECEPTION)")
    public ResponseEntity<BookingDto> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.confirm(id));
    }

    @PostMapping("/{id}/checkin")
    @Operation(summary = "Check-in (RECEPTION)")
    public ResponseEntity<BookingDto> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.checkIn(id));
    }

    // checkOut запускает цепочку: COMPLETED + BookingCompletedEvent → billing-service создаёт финальный счёт.
    @PostMapping("/{id}/checkout")
    @Operation(summary = "Check-out (RECEPTION)")
    public ResponseEntity<BookingDto> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.checkOut(id));
    }
}
