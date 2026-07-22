package com.lavarapido.backend_vehicular.users.dto;

import com.lavarapido.backend_vehicular.shared.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDTO {

    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private DocumentType documentType;
    private String documentNumber;
    private String password;
}
