package com.pethotel.room.controller;

import com.pethotel.room.dto.RoomAvailabilityRequest;
import com.pethotel.room.dto.RoomDto;
import com.pethotel.room.dto.RoomRequest;
import com.pethotel.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms")
public class RoomController {

    private final RoomService roomService;

    // GET /api/rooms/search?checkIn=2025-06-01&checkOut=2025-06-05&guests=2
    // @DateTimeFormat(iso = DATE) — Spring конвертирует строку ISO-8601 ("2025-06-01") в LocalDate.
    //   Без этой аннотации Spring не знает формат и вернёт 400 Bad Request.
    // defaultValue = "1" — если параметр guests не передан, ищем номера хотя бы на 1 гостя.
    @GetMapping("/search")
    @Operation(summary = "Search available rooms")
    public ResponseEntity<List<RoomDto>> search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int guests) {
        return ResponseEntity.ok(roomService.findAvailable(checkIn, checkOut, guests));
    }

    @GetMapping
    @Operation(summary = "Get all rooms (ADMIN, RECEPTION)")
    public ResponseEntity<List<RoomDto>> getAll() {
        return ResponseEntity.ok(roomService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by id")
    public ResponseEntity<RoomDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getById(id));
    }

    // POST /api/rooms — создать новый номер.
    // ResponseEntity.status(CREATED) → 201 Created с телом ответа.
    // 201 (а не 200) — семантически правильно: ресурс создан, а не просто запрос выполнен.
    @PostMapping
    @Operation(summary = "Create room (ADMIN)")
    public ResponseEntity<RoomDto> create(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update room (ADMIN)")
    public ResponseEntity<RoomDto> update(@PathVariable Long id,
                                           @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.update(id, request));
    }

    // DELETE /api/rooms/{id} — удалить номер.
    // ResponseEntity.noContent() → 204 No Content (нет тела ответа).
    // 204 — стандарт REST для успешного DELETE: удалять нечего возвращать.
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete room (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
