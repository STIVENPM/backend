package com.lavarapido.backend_vehicular.auth.repository;

import com.lavarapido.backend_vehicular.auth.entity.TokenRecuperacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Guarda los tokens de recuperación por hash y permite bloquearlos o invalidarlos durante el cambio de contraseña. */
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, UUID> {

    // Bloquea la fila mientras se valida y consume el token de un solo uso.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    // Comprueba si hay tokens sin usar; esta consulta no revisa la expiración.
    boolean existsByUsuario_UserIdAndUsadoFalse(UUID userId);

    // Una nueva solicitud marca como usados todos los tokens anteriores sin usar.
    @Modifying(flushAutomatically = true)
    @Query("update TokenRecuperacion token set token.usado = true "
            + "where token.usuario.userId = :userId and token.usado = false")
    int invalidarTokensActivosPorUsuario(@Param("userId") UUID userId);
}
