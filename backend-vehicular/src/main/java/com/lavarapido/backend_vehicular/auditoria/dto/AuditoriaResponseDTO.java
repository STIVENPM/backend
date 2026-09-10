package com.lavarapido.backend_vehicular.auditoria.dto;
import com.lavarapido.backend_vehicular.auditoria.enums.*;
import java.time.LocalDateTime;
import java.util.UUID;
public record AuditoriaResponseDTO(UUID idAuditoria, UUID usuarioId, AccionAuditoria accion, String descripcion, String ipOrigen, ModuloAuditoria modulo, LocalDateTime createdAt) { }
