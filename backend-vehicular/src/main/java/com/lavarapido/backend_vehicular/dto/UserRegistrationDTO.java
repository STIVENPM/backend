package com.lavarapido.backend_vehicular.dto;

import com.lavarapido.backend_vehicular.enums.DocumentType;
import lombok.*;

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