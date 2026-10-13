package com.lavarapido.backend_vehicular.notificaciones.repository;

import com.lavarapido.backend_vehicular.notificaciones.entity.Notificacion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Consulta notificaciones por usuario y permite contarlas o marcarlas como leídas. */
public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {
    // Lista primero las notificaciones más recientes del usuario.
    Page<Notificacion> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId, Pageable pageable);
    // Filtra las no leídas para la bandeja y el contador del cliente.
    Page<Notificacion> findByUsuarioIdAndLeidaAtIsNullOrderByCreatedAtDesc(UUID usuarioId, Pageable pageable);
    long countByUsuarioIdAndLeidaAtIsNull(UUID usuarioId);
    // Exige coincidencia de ID y propietario al abrir o marcar una notificación.
    Optional<Notificacion> findByIdAndUsuarioId(UUID id, UUID usuarioId);
    // La clave de evento permite evitar una notificación repetida para el usuario.
    boolean existsByUsuarioIdAndClaveEvento(UUID usuarioId, String claveEvento);
    // Marca de una vez todas las no leídas del usuario; devuelve cuántas cambió.
    @Modifying
    @Query("update Notificacion n set n.leidaAt = CURRENT_TIMESTAMP where n.usuarioId = :usuarioId and n.leidaAt is null")
    int marcarTodas(@Param("usuarioId") UUID usuarioId);
}
