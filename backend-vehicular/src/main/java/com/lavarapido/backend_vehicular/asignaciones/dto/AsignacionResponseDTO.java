package com.lavarapido.backend_vehicular.asignaciones.dto;

import com.lavarapido.backend_vehicular.asignaciones.enums.EstadoAsignacion;

import java.time.LocalDateTime;
import java.util.UUID;

public record AsignacionResponseDTO(
        UUID idAsignacion,
        UUID idReserva,
        UUID idOperador,
        String nombreOperador,
        EstadoAsignacion estado,
        LocalDateTime fechaAsignacion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
