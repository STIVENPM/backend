package com.lavarapido.backend_vehicular.operadores.repository;

import com.lavarapido.backend_vehicular.operadores.entity.Operador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OperadorRepository extends JpaRepository<Operador, UUID> {

    boolean existsByUsuario_UserId(UUID userId);

    Optional<Operador> findByUsuario_UserId(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Operador o SET o.estado = false, o.updatedAt = CURRENT_TIMESTAMP WHERE o.estado = true")
    int desactivarTodosLosActivos();
}
