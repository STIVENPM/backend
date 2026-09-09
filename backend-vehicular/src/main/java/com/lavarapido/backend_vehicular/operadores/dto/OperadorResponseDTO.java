package com.lavarapido.backend_vehicular.operadores.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OperadorResponseDTO(
        UUID idOperador,
        UUID idUsuario,
        String firstName,
        String lastName,
        String emailUsuario,
        Boolean estado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
