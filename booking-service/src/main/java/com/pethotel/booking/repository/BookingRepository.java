package com.pethotel.booking.repository;

import com.pethotel.booking.entity.Booking;
import com.pethotel.common.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerId(Long customerId);
    List<Booking> findByStatus(BookingStatus status);
}
