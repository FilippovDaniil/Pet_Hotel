package com.pethotel.amenity.repository;

import com.pethotel.amenity.entity.Amenity;
import com.pethotel.common.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// @Repository — явная аннотация; для интерфейсов Spring Data она необязательна
// (Spring Data сам регистрирует реализацию), но улучшает читаемость кода.
@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    // Derived query: SELECT * FROM amenity.amenities WHERE type = :type
    // Позволяет получить, например, все услуги типа SAUNA (их может быть несколько — разные сауны).
    List<Amenity> findByType(ServiceType type);
}
