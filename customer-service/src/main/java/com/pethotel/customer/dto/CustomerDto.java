package com.pethotel.customer.dto;

import com.pethotel.common.enums.Role;
import lombok.Data;

@Data
public class CustomerDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Role role;
}
