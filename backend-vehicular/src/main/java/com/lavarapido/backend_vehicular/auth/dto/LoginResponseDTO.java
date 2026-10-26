package com.lavarapido.backend_vehicular.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Devuelve el JWT y los datos públicos del usuario después de un login correcto; no devuelve la contraseña. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    // Se envía en Authorization: Bearer <token> para acceder a rutas protegidas.
    private String token;
    private UserInfoDTO user;

    /** Datos públicos de la cuenta y rol vigente que el cliente muestra tras iniciar sesión. */
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
