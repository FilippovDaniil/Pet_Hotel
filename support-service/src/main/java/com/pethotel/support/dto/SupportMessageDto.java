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
public class SupportMessageDto {
    private Long id;
    private Long customerId;
    private String customerEmail;
    private String senderRole;
    private String content;
    private LocalDateTime createdAt;
    private boolean readByCustomer;
    private boolean readByAdmin;
}
