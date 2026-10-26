package com.lavarapido.backend_vehicular.calificaciones.dto;
import java.time.LocalDateTime;
import java.util.UUID;
/** Devuelve la calificación guardada con sus identificadores y fechas de creación y actualización. */
public record CalificacionResponseDTO(UUID idCalificacion, UUID reservaId, UUID usuarioId, Integer puntuacion, String comentario, LocalDateTime createdAt, LocalDateTime updatedAt) { }
