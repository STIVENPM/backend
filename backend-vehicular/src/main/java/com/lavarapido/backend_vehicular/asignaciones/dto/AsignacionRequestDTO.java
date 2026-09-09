package com.lavarapido.backend_vehicular.asignaciones.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AsignacionRequestDTO(
        @NotNull(message = "La reserva es obligatoria")
        UUID idReserva,

        @NotNull(message = "El operador es obligatorio")
        UUID idOperador
) {
}
