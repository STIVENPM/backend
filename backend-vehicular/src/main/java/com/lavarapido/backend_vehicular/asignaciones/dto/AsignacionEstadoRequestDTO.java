package com.lavarapido.backend_vehicular.asignaciones.dto;

import com.lavarapido.backend_vehicular.asignaciones.enums.EstadoAsignacion;
import jakarta.validation.constraints.NotNull;

public record AsignacionEstadoRequestDTO(
        @NotNull(message = "El estado es obligatorio")
        EstadoAsignacion estado
) {
}
