package com.pethotel.customer.dto;

import com.pethotel.common.enums.Role;
import lombok.Data;

// DTO профиля клиента — возвращается в ответах GET /api/customers/*.
// Намеренно не включает passwordHash: этот field Entity-класса Customer
// никогда не должен выходить за пределы сервиса.
//
// @Data с сеттерами нужен для ручного маппинга в CustomerService.toDto():
//   dto.setId(c.getId()); dto.setEmail(c.getEmail()); ...
// Альтернатива — MapStruct или ModelMapper, но для учебного проекта явный маппинг нагляднее.
@Data
public class CustomerDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Role role;   // enum: Jackson сериализует его в строку "CUSTOMER" и т.д.
}
