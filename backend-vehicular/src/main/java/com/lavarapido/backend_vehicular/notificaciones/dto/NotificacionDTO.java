package com.lavarapido.backend_vehicular.notificaciones.dto;

import com.lavarapido.backend_vehicular.notificaciones.entity.Notificacion;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacionDTO(UUID id, String tipo, String titulo, String mensaje,
                              UUID reservaId, UUID pagoId, LocalDateTime leidaAt, LocalDateTime createdAt) {
    public static NotificacionDTO from(Notificacion n) {
        return new NotificacionDTO(n.id, n.tipo, n.titulo, n.mensaje,
                n.reservaId, n.pagoId, n.leidaAt, n.createdAt);
    }
}
