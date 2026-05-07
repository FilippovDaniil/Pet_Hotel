package com.pethotel.billing.repository;

import com.pethotel.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Derived Query: SELECT i FROM Invoice i WHERE i.bookingId = ?1
    // Optional: один bookingId → максимум один Invoice (бизнес-инвариант)
    Optional<Invoice> findByBookingId(Long bookingId);

    // Все счета клиента (история); CUSTOMER видит только свои через X-User-Id в контроллере
    List<Invoice> findByCustomerId(Long customerId);
}
