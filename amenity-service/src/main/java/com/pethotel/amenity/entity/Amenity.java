package com.pethotel.amenity.entity;

import com.pethotel.common.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "amenities", schema = "amenity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultPrice;

    @Column(nullable = false)
    private int maxDurationMinutes;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean available = true;

    @Column(columnDefinition = "bytea")
    private byte[] imageData;

    @Column(length = 100)
    private String imageMimeType;
}
