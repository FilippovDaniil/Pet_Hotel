package com.pethotel.amenity.service;

import com.pethotel.amenity.dto.AmenityDto;
import com.pethotel.amenity.dto.AmenityRequest;
import com.pethotel.amenity.entity.Amenity;
import com.pethotel.amenity.repository.AmenityRepository;
import com.pethotel.common.enums.ServiceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmenityService {

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
        return dto;
    }
}
