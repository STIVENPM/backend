package com.lavarapido.backend_vehicular.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/** Recibe la contraseña actual y el correo nuevo para la cuenta autenticada. */
public record ChangeEmailRequestDTO(
        @NotBlank(message = "La contraseña actual es obligatoria") String currentPassword,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 100, message = "El correo no puede superar los 100 caracteres")
        String newEmail) {

    public ChangeEmailRequestDTO {
        if (newEmail != null) {
            newEmail = newEmail.trim().toLowerCase(Locale.ROOT);
        }
    }
}
