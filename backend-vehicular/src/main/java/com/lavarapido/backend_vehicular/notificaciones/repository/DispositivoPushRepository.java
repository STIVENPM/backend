package com.lavarapido.backend_vehicular.notificaciones.repository;

import com.lavarapido.backend_vehicular.notificaciones.entity.DispositivoPush;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Busca instalaciones y tokens push asociados a los usuarios para entregar notificaciones. */
public interface DispositivoPushRepository extends JpaRepository<DispositivoPush, UUID> {
    // Obtiene los dispositivos habilitados a los que se puede intentar enviar un push.
    List<DispositivoPush> findByUsuarioIdAndActivoTrue(UUID usuarioId);
    // Reconoce una instalación concreta para actualizar su token sin duplicarla.
    Optional<DispositivoPush> findByUsuarioIdAndInstalacionId(UUID usuarioId, String instalacionId);
    // Localiza el dispositivo que tiene ese token push.
    Optional<DispositivoPush> findByToken(String token);
}
