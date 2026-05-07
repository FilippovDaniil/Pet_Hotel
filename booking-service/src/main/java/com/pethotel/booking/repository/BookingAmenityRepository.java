package com.pethotel.booking.repository;

import com.pethotel.booking.entity.BookingAmenity;
import com.pethotel.common.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingAmenityRepository extends JpaRepository<BookingAmenity, Long> {

    // Детектор пересечений временных слотов для конкретного типа услуги.
    //
    // Классическое условие перекрытия двух интервалов [A.start, A.end) и [B.start, B.end):
    //   A.start < B.end  AND  B.start < A.end
    //
    // То есть: интервалы пересекаются тогда и только тогда, когда каждый из них начинается
    // раньше, чем заканчивается другой. Этот предикат работает для всех видов пересечений
    // (частичное, полное включение).
    //
    // Если список не пустой — слот занят, BookingService выбросит IllegalStateException.
    @Query("""
        SELECT ba FROM BookingAmenity ba
        WHERE ba.serviceType = :type
        AND ba.startTime < :endTime AND ba.endTime > :startTime
    """)
    List<BookingAmenity> findConflicting(@Param("type") ServiceType type,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
}
