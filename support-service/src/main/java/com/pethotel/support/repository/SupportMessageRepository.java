package com.pethotel.support.repository;

import com.pethotel.support.entity.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    List<SupportMessage> findByCustomerIdOrderByCreatedAtAsc(Long customerId);

    Optional<SupportMessage> findTopByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("SELECT DISTINCT m.customerId FROM SupportMessage m")
    List<Long> findDistinctCustomerIds();

    long countByCustomerIdAndSenderRoleAndReadByAdminFalse(Long customerId, String senderRole);

    long countByCustomerIdAndSenderRoleAndReadByCustomerFalse(Long customerId, String senderRole);

    @Modifying
    @Transactional
    @Query("UPDATE SupportMessage m SET m.readByAdmin = true WHERE m.customerId = :customerId AND m.senderRole = 'CUSTOMER'")
    void markCustomerMessagesAsReadByAdmin(@Param("customerId") Long customerId);

    @Modifying
    @Transactional
    @Query("UPDATE SupportMessage m SET m.readByCustomer = true WHERE m.customerId = :customerId AND m.senderRole = 'ADMIN'")
    void markAdminMessagesAsReadByCustomer(@Param("customerId") Long customerId);
}
