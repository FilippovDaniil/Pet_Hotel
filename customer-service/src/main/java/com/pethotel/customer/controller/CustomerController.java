package com.pethotel.customer.controller;

import com.pethotel.common.enums.Role;
import com.pethotel.customer.dto.CustomerDto;
import com.pethotel.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    @Operation(summary = "Get current customer profile")
    public ResponseEntity<CustomerDto> getMe(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(customerService.getById(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by id (ADMIN, RECEPTION)")
    public ResponseEntity<CustomerDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Get all customers (ADMIN)")
    public ResponseEntity<List<CustomerDto>> getAll() {
        return ResponseEntity.ok(customerService.getAll());
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Update customer role (ADMIN)")
    public ResponseEntity<CustomerDto> updateRole(@PathVariable Long id,
                                                   @RequestParam Role role) {
        return ResponseEntity.ok(customerService.updateRole(id, role));
    }
}
