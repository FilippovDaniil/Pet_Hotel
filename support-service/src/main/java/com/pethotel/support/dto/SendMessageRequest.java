package com.pethotel.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Request DTO — приходит от клиента в теле HTTP-запроса (POST /api/support/messages).
// Содержит только то, что клиент вправе задать. Все остальные поля сообщения
// (customerId, createdAt, senderRole) проставляются на сервере — клиент не должен их контролировать.

@Data  // @Getter + @Setter + boilerplate (достаточно для request DTO)
public class SendMessageRequest {

    // @NotBlank — проверяет, что строка не null, не пустая ("") и не состоит только из пробелов.
    // Валидация срабатывает благодаря @Valid в контроллере; при ошибке GlobalExceptionHandler
    // перехватывает MethodArgumentNotValidException и возвращает 400 с текстом ошибки.
    @NotBlank(message = "Сообщение не может быть пустым")

    // @Size(max=2000) — ограничение длины. Без него злоумышленник мог бы отправить
    // запрос с мегабайтным телом и "засорить" БД. Ограничение согласовано с Entity (TEXT).
    @Size(max = 2000, message = "Сообщение не должно превышать 2000 символов")
    private String content;
}
