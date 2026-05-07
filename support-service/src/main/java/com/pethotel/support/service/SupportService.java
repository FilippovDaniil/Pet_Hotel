package com.pethotel.support.service;

import com.pethotel.support.dto.ConversationSummaryDto;
import com.pethotel.support.dto.SupportMessageDto;
import com.pethotel.support.entity.SupportMessage;
import com.pethotel.support.repository.SupportMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportService {

    private final SupportMessageRepository repository;

    public List<SupportMessageDto> getCustomerMessages(Long customerId) {
        return repository.findByCustomerIdOrderByCreatedAtAsc(customerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public SupportMessageDto sendCustomerMessage(Long customerId, String customerEmail, String content) {
        SupportMessage message = SupportMessage.builder()
                .customerId(customerId)
                .customerEmail(customerEmail)
                .senderRole("CUSTOMER")
                .content(content)
                .createdAt(LocalDateTime.now())
                .readByCustomer(true)
                .readByAdmin(false)
                .build();
        SupportMessage saved = repository.save(message);
        log.info("Customer {} sent support message id={}", customerId, saved.getId());
        return toDto(saved);
    }

    public long getUnreadCountForCustomer(Long customerId) {
        return repository.countByCustomerIdAndSenderRoleAndReadByCustomerFalse(customerId, "ADMIN");
    }

    @Transactional
    public void markAsReadByCustomer(Long customerId) {
        repository.markAdminMessagesAsReadByCustomer(customerId);
    }

    public List<ConversationSummaryDto> getConversationSummaries() {
        List<Long> customerIds = repository.findDistinctCustomerIds();
        return customerIds.stream()
                .map(customerId -> repository.findTopByCustomerIdOrderByCreatedAtDesc(customerId)
                        .map(last -> {
                            long unreadByAdmin = repository.countByCustomerIdAndSenderRoleAndReadByAdminFalse(customerId, "CUSTOMER");
                            long unreadByCustomer = repository.countByCustomerIdAndSenderRoleAndReadByCustomerFalse(customerId, "ADMIN");
                            return ConversationSummaryDto.builder()
                                    .customerId(customerId)
                                    .customerEmail(last.getCustomerEmail())
                                    .lastMessage(last.getContent())
                                    .lastMessageAt(last.getCreatedAt())
                                    .unreadByAdmin(unreadByAdmin)
                                    .unreadByCustomer(unreadByCustomer)
                                    .build();
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ConversationSummaryDto::getLastMessageAt).reversed())
                .toList();
    }

    public List<SupportMessageDto> getAdminConversation(Long customerId) {
        return repository.findByCustomerIdOrderByCreatedAtAsc(customerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public SupportMessageDto sendAdminMessage(Long customerId, String content) {
        String customerEmail = repository.findTopByCustomerIdOrderByCreatedAtDesc(customerId)
                .map(SupportMessage::getCustomerEmail)
                .orElseThrow(() -> new NoSuchElementException("Диалог с клиентом " + customerId + " не найден"));

        SupportMessage message = SupportMessage.builder()
                .customerId(customerId)
                .customerEmail(customerEmail)
                .senderRole("ADMIN")
                .content(content)
                .createdAt(LocalDateTime.now())
                .readByCustomer(false)
                .readByAdmin(true)
                .build();
        SupportMessage saved = repository.save(message);
        log.info("Admin replied to customer {} message id={}", customerId, saved.getId());
        return toDto(saved);
    }

    @Transactional
    public void markAsReadByAdmin(Long customerId) {
        repository.markCustomerMessagesAsReadByAdmin(customerId);
    }

    private SupportMessageDto toDto(SupportMessage m) {
        return SupportMessageDto.builder()
                .id(m.getId())
                .customerId(m.getCustomerId())
                .customerEmail(m.getCustomerEmail())
                .senderRole(m.getSenderRole())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .readByCustomer(m.isReadByCustomer())
                .readByAdmin(m.isReadByAdmin())
                .build();
    }
}
