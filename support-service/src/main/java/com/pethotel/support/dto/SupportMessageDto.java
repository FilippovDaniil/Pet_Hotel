package com.pethotel.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// DTO (Data Transfer Object) — объект для передачи данных между слоями и на фронтенд.
// Принцип: контроллер никогда не возвращает Entity напрямую.
// Причины:
//   1. Entity может содержать поля, которые не нужны клиенту (или небезопасно раскрывать)
//   2. Entity "прибита" к схеме БД — DTO даёт свободу менять один, не трогая другой
//   3. Jackson сериализует DTO в JSON; у Entity могут быть lazy-коллекции, которые вызовут ошибку вне транзакции

// @Data = @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor
@Data
@Builder          // для удобного создания в SupportService.toDto()
@NoArgsConstructor   // нужен Jackson для десериализации (если DTO придёт в запросе)
@AllArgsConstructor  // нужен @Builder
public class SupportMessageDto {

    private Long id;
    private Long customerId;
    private String customerEmail;

    // Роль отправителя — "CUSTOMER" или "ADMIN".
    // Фронтенд использует это для выравнивания пузырей: CUSTOMER → вправо, ADMIN → влево.
    private String senderRole;

    private String content;

    // LocalDateTime → Jackson сериализует в ISO-8601: "2025-06-01T10:00:00"
    // Фронтенд форматирует это под локаль пользователя (new Date(iso).toLocaleString(...))
    private LocalDateTime createdAt;

    // Флаги для UI "прочитано/не прочитано" — аналог галочек в мессенджерах
    private boolean readByCustomer;
    private boolean readByAdmin;
}
