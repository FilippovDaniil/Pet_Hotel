package com.pethotel.dining.repository;

import com.pethotel.dining.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Все заказы по бронированию — используется для отображения истории заказов в карточке бронирования.
    List<Order> findByBookingId(Long bookingId);

    // Заказы клиента за временной период — потенциально для будущей отчётности или фильтрации.
    // Derived query: WHERE customer_id = :customerId AND order_time BETWEEN :start AND :end
    List<Order> findByCustomerIdAndOrderTimeBetween(Long customerId, LocalDateTime start, LocalDateTime end);

    // История заказов клиента, отсортированная от новых к старым.
    // OrderByOrderTimeDesc — суффикс Spring Data: добавляет ORDER BY order_time DESC в SQL.
    List<Order> findByCustomerIdOrderByOrderTimeDesc(Long customerId);
}
