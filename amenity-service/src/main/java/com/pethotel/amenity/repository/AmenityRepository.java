package com.pethotel.amenity.repository;

import com.pethotel.amenity.entity.Amenity;
import com.pethotel.common.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    List<Amenity> findByType(ServiceType type);
}
