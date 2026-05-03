package com.pethotel.dining.controller;

import com.pethotel.dining.dto.MenuItemDto;
import com.pethotel.dining.dto.MenuItemRequest;
import com.pethotel.dining.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Tag(name = "Menu")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    @Operation(summary = "Get all menu items")
    public ResponseEntity<List<MenuItemDto>> getAll() {
        return ResponseEntity.ok(menuService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get menu item by id")
    public ResponseEntity<MenuItemDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create menu item (ADMIN)")
    public ResponseEntity<MenuItemDto> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update menu item (ADMIN)")
    public ResponseEntity<MenuItemDto> update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete menu item (ADMIN)")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        menuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
