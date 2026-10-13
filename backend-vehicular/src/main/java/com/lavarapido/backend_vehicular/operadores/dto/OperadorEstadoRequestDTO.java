package com.lavarapido.backend_vehicular.operadores.dto;

import jakarta.validation.constraints.NotNull;

/** Recibe el estado activo o inactivo que se asignará a un operador. */
public record OperadorEstadoRequestDTO(
        @NotNull(message = "El estado es obligatorio")
        Boolean estado
) {
}
