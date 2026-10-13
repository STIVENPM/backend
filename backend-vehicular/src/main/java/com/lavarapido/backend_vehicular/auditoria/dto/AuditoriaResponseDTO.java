package com.lavarapido.backend_vehicular.auditoria.dto;
import com.lavarapido.backend_vehicular.auditoria.enums.*;
import java.time.LocalDateTime;
import java.util.UUID;
/** Devuelve un evento de auditoría: quién actuó, qué hizo, en qué módulo, desde qué IP y cuándo. */
public record AuditoriaResponseDTO(UUID idAuditoria, UUID usuarioId, AccionAuditoria accion, String descripcion, String ipOrigen, ModuloAuditoria modulo, LocalDateTime createdAt) { }
