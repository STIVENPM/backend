package com.lavarapido.backend_vehicular.repository;

import com.lavarapido.backend_vehicular.entities.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, UUID> {

    // Busca un token por su hash para validarlo cuando el usuario hace clic en el enlace
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    // Verifica si un usuario ya tiene tokens activos (no usados y no expirados)
    // útil para evitar spam de correos
    boolean existsByUsuario_UserIdAndUsadoFalse(UUID userId);
}
