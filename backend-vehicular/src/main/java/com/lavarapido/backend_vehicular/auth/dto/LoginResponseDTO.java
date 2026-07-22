package com.lavarapido.backend_vehicular.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;
    private UserInfoDTO user;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDTO {

        private String userId;
        private String firstName;
        private String email;
        private String role;
    }
}
