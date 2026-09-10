package com.lavarapido.backend_vehicular.calificaciones.dto;
import java.time.LocalDateTime;
import java.util.UUID;
public record CalificacionResponseDTO(UUID idCalificacion, UUID reservaId, UUID usuarioId, Integer puntuacion, String comentario, LocalDateTime createdAt, LocalDateTime updatedAt) { }
