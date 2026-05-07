package com.pethotel.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDto {
    private Long customerId;
    private String customerEmail;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadByAdmin;
    private long unreadByCustomer;
}
