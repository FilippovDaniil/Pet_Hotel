package com.pethotel.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO (Data Transfer Object) — объект для передачи данных из HTTP-запроса в сервис.
// Не является Entity: не имеет @Id, не хранится в БД напрямую.
// Разделение DTO / Entity позволяет:
//   - добавлять поля в API без изменения схемы БД и наоборот
//   - скрывать внутренние поля (например passwordHash никогда не попадает в запрос)
//
// @Data — Lombok: генерирует геттеры, сеттеры, equals, hashCode, toString.
//         Необходим, чтобы Jackson мог десериализовать JSON через сеттеры.
@Data
public class RegisterRequest {

    // @Email     — поле должно соответствовать формату email@domain.com
    // @NotBlank  — поле не может быть null, пустой строкой или состоять только из пробелов
    @Email @NotBlank
    private String email;

    // @Size(min = 6) — пароль не короче 6 символов.
    // Проверка выполняется до передачи объекта в CustomerService (@Valid в контроллере).
    // Если валидация не прошла — Spring бросает MethodArgumentNotValidException → 400 Bad Request.
    @NotBlank @Size(min = 6)
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    // Телефон необязателен — нет @NotBlank, Jackson проставит null если поле отсутствует в JSON.
    private String phone;
}
