package com.lavarapido.backend_vehicular.marcas.dto;


import java.time.LocalDateTime;
import java.util.UUID;

public record MarcaResponseDTO(
    UUID idMarca,
    String nombre,
    Boolean estado,
    // Datos de quién la sugirió, útil para el admin al revisar pendientes.
    // Ambos quedan null si la marca fue creada directamente por el admin.
    UUID idUsuarioSolicitante,
    String emailUsuarioSolicitante,
    LocalDateTime createdAt
) {}