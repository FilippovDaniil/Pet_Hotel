package com.pethotel.room.dto;

import com.pethotel.common.enums.RoomClass;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomDto {
    private Long id;
    private String roomNumber;
    private RoomClass roomClass;
    private Integer capacity;
    private BigDecimal pricePerNight;
    private String description;
}
