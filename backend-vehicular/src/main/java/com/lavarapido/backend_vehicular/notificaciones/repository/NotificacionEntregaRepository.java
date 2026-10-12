package com.lavarapido.backend_vehicular.notificaciones.repository;

import com.lavarapido.backend_vehicular.notificaciones.entity.NotificacionEntrega;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificacionEntregaRepository extends JpaRepository<NotificacionEntrega, UUID> {
    @Query(value = "SELECT * FROM notificacion_entregas WHERE estado = 'pendiente' AND proxima_ejecucion <= CURRENT_TIMESTAMP ORDER BY proxima_ejecucion, id_entrega LIMIT 20 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<NotificacionEntrega> reclamarPendientes();
    @Query(value = "SELECT * FROM notificacion_entregas WHERE canal = 'push' AND estado = 'aceptada' AND proxima_ejecucion <= CURRENT_TIMESTAMP ORDER BY proxima_ejecucion, id_entrega LIMIT 20 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<NotificacionEntrega> reclamarRecibos();
}
