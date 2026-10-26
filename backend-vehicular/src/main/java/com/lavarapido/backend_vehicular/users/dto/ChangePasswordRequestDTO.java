package com.lavarapido.backend_vehicular.users.dto;

import jakarta.validation.constraints.NotBlank;

/** Recibe la contraseña actual y la nueva para cambiarla en la cuenta autenticada. */
public record ChangePasswordRequestDTO(
        @NotBlank(message = "La contraseña actual es obligatoria") String currentPassword,
        @NotBlank(message = "La nueva contraseña es obligatoria") String newPassword) {
}
