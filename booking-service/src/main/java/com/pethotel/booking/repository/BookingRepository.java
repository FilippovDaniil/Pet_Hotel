package com.pethotel.booking.repository;

import com.pethotel.booking.entity.Booking;
import com.pethotel.common.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Derived queries — Spring Data генерирует SQL из имён методов.
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // SELECT * FROM booking.bookings WHERE customer_id = :customerId
    // Используется для GET /api/bookings/my — список бронирований текущего клиента.
    List<Booking> findByCustomerId(Long customerId);

    // SELECT * FROM booking.bookings WHERE status = :status
    // Полезен для административных запросов: "все PENDING-бронирования для подтверждения".
    List<Booking> findByStatus(BookingStatus status);
}
