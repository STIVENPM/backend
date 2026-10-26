package com.lavarapido.backend_vehicular.operadores.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Identifica por correo y documento a la cuenta existente que se convertirá en operador. */
public record OperadorRequestDTO(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String email,

        @NotBlank(message = "El número de documento es obligatorio")
        String documentNumber
) {
}
