package com.lavarapido.backend_vehicular.log_errores.dto;
import com.lavarapido.backend_vehicular.log_errores.enums.TipoError;
import java.time.LocalDateTime;
import java.util.UUID;
public record LogErrorResponseDTO(UUID idError, UUID usuarioId, TipoError tipoError, String descripcion, String ipOrigen, Boolean resuelto, LocalDateTime createdAt, LocalDateTime updatedAt) { }
