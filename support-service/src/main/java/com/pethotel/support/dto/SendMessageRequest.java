package com.pethotel.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank(message = "Сообщение не может быть пустым")
    @Size(max = 2000, message = "Сообщение не должно превышать 2000 символов")
    private String content;
}
