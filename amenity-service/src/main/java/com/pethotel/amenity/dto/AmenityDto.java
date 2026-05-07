package com.pethotel.amenity.dto;

import com.pethotel.common.enums.ServiceType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AmenityDto {
    private Long id;
    private String name;
    private ServiceType type;
    private BigDecimal defaultPrice;
    private int maxDurationMinutes;
    private String description;
    private boolean available;
    private boolean hasImage;
}
