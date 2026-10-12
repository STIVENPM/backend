package com.lavarapido.backend_vehicular.notificaciones.repository;

import com.lavarapido.backend_vehicular.notificaciones.entity.DispositivoPush;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispositivoPushRepository extends JpaRepository<DispositivoPush, UUID> {
    List<DispositivoPush> findByUsuarioIdAndActivoTrue(UUID usuarioId);
    Optional<DispositivoPush> findByUsuarioIdAndInstalacionId(UUID usuarioId, String instalacionId);
    Optional<DispositivoPush> findByToken(String token);
}
