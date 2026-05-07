package com.pethotel.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// ConversationSummaryDto — краткая сводка по диалогу для списка в левой панели AdminSupportPage.
// Одна запись на клиента: не все сообщения, а только итоговая информация.
// Это позволяет admin сразу видеть:
//   - кто написал (customerEmail)
//   - последнее сообщение (lastMessage — превью)
//   - когда (lastMessageAt — для сортировки)
//   - сколько непрочитанных (unreadByAdmin — красный бейдж)

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDto {

    // ID клиента — используется фронтендом для запроса полного диалога
    // GET /api/support/admin/conversations/{customerId}
    private Long customerId;

    // Email отображается в списке вместо "Customer #123" — так понятнее
    private String customerEmail;

    // Текст последнего сообщения (из любого отправителя).
    // На фронтенде обрезается через CSS truncate — показывает первые ~40 символов.
    private String lastMessage;

    // Время последнего сообщения — используется для:
    //   1. Сортировки списка (новые диалоги сверху)
    //   2. Отображения времени рядом с превью ("14:30" или "вчера")
    private LocalDateTime lastMessageAt;

    // Кол-во сообщений от клиента, которые admin ещё не прочёл.
    // Отображается в красном круглом бейдже в списке диалогов.
    private long unreadByAdmin;

    // Кол-во ответов admin, которые клиент ещё не видел.
    // Может использоваться для бейджа на вкладке "Поддержка" у клиента (не реализовано).
    private long unreadByCustomer;
}
