package com.lavarapido.backend_vehicular.notificaciones.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Devuelve una página de notificaciones junto con su número, tamaño y totales. */
public record PaginaNotificacionesDTO(List<NotificacionDTO> content, int number, int size,
                                      long totalElements, int totalPages) {
    // Adapta la página de Spring; number empieza en 0.
    public static PaginaNotificacionesDTO from(Page<NotificacionDTO> page) {
        return new PaginaNotificacionesDTO(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
