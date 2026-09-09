package com.lavarapido.backend_vehicular.operadores.dto;

import jakarta.validation.constraints.NotNull;

public record OperadorEstadoRequestDTO(
        @NotNull(message = "El estado es obligatorio")
        Boolean estado
) {
}
