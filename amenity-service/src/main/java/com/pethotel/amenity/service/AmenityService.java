package com.pethotel.amenity.service;

import com.pethotel.amenity.dto.AmenityDto;
import com.pethotel.amenity.dto.AmenityRequest;
import com.pethotel.amenity.entity.Amenity;
import com.pethotel.amenity.repository.AmenityRepository;
import com.pethotel.common.enums.ServiceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmenityService {

    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;

    private final AmenityRepository amenityRepository;

    public List<AmenityDto> getAll() {
        log.info("Fetching all amenities");
        return amenityRepository.findAll().stream().map(this::toDto).toList();
    }

    public AmenityDto getById(Long id) {
        log.info("Fetching amenity by id={}", id);
        return toDto(findAmenity(id));
    }

    public List<AmenityDto> getByType(ServiceType type) {
        log.info("Fetching amenities by type={}", type);
        return amenityRepository.findByType(type).stream().map(this::toDto).toList();
    }

    @Transactional
    public AmenityDto create(AmenityRequest request) {
        Amenity amenity = Amenity.builder()
                .name(request.getName())
                .type(request.getType())
                .defaultPrice(request.getDefaultPrice())
                .maxDurationMinutes(request.getMaxDurationMinutes())
                .description(request.getDescription())
                .available(request.isAvailable())
                .build();
        amenity = amenityRepository.save(amenity);
        log.info("Amenity created: id={} name={} type={}", amenity.getId(), amenity.getName(), amenity.getType());
        return toDto(amenity);
    }

    @Transactional
    public AmenityDto update(Long id, AmenityRequest request) {
        Amenity amenity = findAmenity(id);
        amenity.setName(request.getName());
        amenity.setType(request.getType());
        amenity.setDefaultPrice(request.getDefaultPrice());
        amenity.setMaxDurationMinutes(request.getMaxDurationMinutes());
        amenity.setDescription(request.getDescription());
        amenity.setAvailable(request.isAvailable());
        log.info("Amenity updated: id={}", id);
        return toDto(amenityRepository.save(amenity));
    }

    @Transactional
    public void delete(Long id) {
        if (!amenityRepository.existsById(id)) {
            throw new NoSuchElementException("Amenity not found: " + id);
        }
        amenityRepository.deleteById(id);
        log.info("Amenity deleted: id={}", id);
    }

    @Transactional
    public void uploadImage(Long id, MultipartFile file) {
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image size must not exceed 2MB");
        }
        Amenity amenity = findAmenity(id);
        try {
            amenity.setImageData(file.getBytes());
            amenity.setImageMimeType(file.getContentType());
            amenityRepository.save(amenity);
            log.info("Image uploaded for amenity id={} size={}", id, file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image data", e);
        }
    }

    public ResponseEntity<byte[]> getImage(Long id) {
        Amenity amenity = findAmenity(id);
        if (amenity.getImageData() == null) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        String mimeType = amenity.getImageMimeType() != null ? amenity.getImageMimeType() : "image/jpeg";
        headers.setContentType(MediaType.parseMediaType(mimeType));
        return new ResponseEntity<>(amenity.getImageData(), headers, HttpStatus.OK);
    }

    private Amenity findAmenity(Long id) {
        return amenityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Amenity not found: " + id));
    }

    private AmenityDto toDto(Amenity a) {
        AmenityDto dto = new AmenityDto();
        dto.setId(a.getId());
        dto.setName(a.getName());
        dto.setType(a.getType());
        dto.setDefaultPrice(a.getDefaultPrice());
        dto.setMaxDurationMinutes(a.getMaxDurationMinutes());
        dto.setDescription(a.getDescription());
        dto.setAvailable(a.isAvailable());
        dto.setHasImage(a.getImageData() != null && a.getImageData().length > 0);
        return dto;
    }
}
