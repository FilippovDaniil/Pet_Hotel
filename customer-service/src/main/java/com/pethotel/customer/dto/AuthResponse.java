package com.pethotel.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// DTO ответа на успешный логин или регистрацию.
// Фронтенд сохраняет эти данные в Zustand-хранилище (auth.store.ts):
//   token  → добавляется в Authorization: Bearer <token> заголовок каждого запроса
//   userId → используется для запросов к своему профилю (/api/customers/me)
//   role   → определяет, какие страницы и кнопки видит пользователь (RequireRole в App.tsx)
//
// @AllArgsConstructor — нужен, потому что в CustomerService используется:
//   new AuthResponse(token, id, email, role.name())
// Ручной конструктор вместо @Builder — объект создаётся в одном месте и всегда полностью заполнен.
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;   // JWT-токен (передаётся в каждом последующем запросе)
    private Long userId;    // ID пользователя в customer-service
    private String email;
    private String role;    // строковое имя: "CUSTOMER", "RECEPTION" или "ADMIN"
}
