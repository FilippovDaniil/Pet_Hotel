package com.pethotel.amenity.controller;

import com.pethotel.amenity.dto.AmenityDto;
import com.pethotel.amenity.dto.AmenityRequest;
import com.pethotel.amenity.service.AmenityService;
import com.pethotel.common.enums.ServiceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
@Tag(name = "Amenities")
public class AmenityController {

    private final AmenityService amenityService;

    @GetMapping
    @Operation(summary = "Get all amenities")
    public ResponseEntity<List<AmenityDto>> getAll() {
        return ResponseEntity.ok(amenityService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get amenity by id")
    public ResponseEntity<AmenityDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(amenityService.getById(id));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get amenities by type")
    public ResponseEntity<List<AmenityDto>> getByType(@PathVariable ServiceType type) {
        return ResponseEntity.ok(amenityService.getByType(type));
    }

    @PostMapping
    @Operation(summary = "Create amenity (ADMIN)")
    public ResponseEntity<AmenityDto> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AmenityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(amenityService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update amenity (ADMIN)")
    public ResponseEntity<AmenityDto> update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody AmenityRequest request) {
        return ResponseEntity.ok(amenityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete amenity (ADMIN)")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        amenityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload amenity image (ADMIN)")
    public ResponseEntity<Void> uploadImage(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        amenityService.uploadImage(id, file);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "Get amenity image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        return amenityService.getImage(id);
    }
}
