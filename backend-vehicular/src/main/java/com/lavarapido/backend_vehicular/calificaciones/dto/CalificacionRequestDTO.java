package com.lavarapido.backend_vehicular.calificaciones.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record CalificacionRequestDTO(
        @NotNull(message = "La reserva es obligatoria") UUID reservaId,
        @NotNull(message = "La puntuacion es obligatoria") @Min(value = 1, message = "La puntuacion debe estar entre 1 y 5") @Max(value = 5, message = "La puntuacion debe estar entre 1 y 5") Integer puntuacion,
        @Size(max = 300, message = "El comentario no puede superar los 300 caracteres") String comentario) { }
