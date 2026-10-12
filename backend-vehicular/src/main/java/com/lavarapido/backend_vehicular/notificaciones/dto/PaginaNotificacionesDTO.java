package com.lavarapido.backend_vehicular.notificaciones.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaNotificacionesDTO(List<NotificacionDTO> content, int number, int size,
                                      long totalElements, int totalPages) {
    public static PaginaNotificacionesDTO from(Page<NotificacionDTO> page) {
        return new PaginaNotificacionesDTO(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
