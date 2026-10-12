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

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {
    Page<Notificacion> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId, Pageable pageable);
    Page<Notificacion> findByUsuarioIdAndLeidaAtIsNullOrderByCreatedAtDesc(UUID usuarioId, Pageable pageable);
    long countByUsuarioIdAndLeidaAtIsNull(UUID usuarioId);
    Optional<Notificacion> findByIdAndUsuarioId(UUID id, UUID usuarioId);
    boolean existsByUsuarioIdAndClaveEvento(UUID usuarioId, String claveEvento);
    @Modifying
    @Query("update Notificacion n set n.leidaAt = CURRENT_TIMESTAMP where n.usuarioId = :usuarioId and n.leidaAt is null")
    int marcarTodas(@Param("usuarioId") UUID usuarioId);
}
