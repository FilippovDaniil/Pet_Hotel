package com.pethotel.dining.repository;

import com.pethotel.dining.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBookingId(Long bookingId);

    List<Order> findByCustomerIdAndOrderTimeBetween(Long customerId, LocalDateTime start, LocalDateTime end);
}
