package com.lavarapido.backend_vehicular.operadores.repository;

import com.lavarapido.backend_vehicular.operadores.entity.Operador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/** Relaciona operadores con cuentas de usuario y permite desactivar los operadores activos. */
public interface OperadorRepository extends JpaRepository<Operador, UUID> {

    // Evita crear dos perfiles de operador para la misma cuenta.
    boolean existsByUsuario_UserId(UUID userId);

    // Recupera el perfil de operador vinculado a una cuenta.
    Optional<Operador> findByUsuario_UserId(UUID userId);

    // Actualización masiva: desactiva solo los operadores aún activos y
    // devuelve la cantidad de filas modificadas.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Operador o SET o.estado = false, o.updatedAt = CURRENT_TIMESTAMP WHERE o.estado = true")
    int desactivarTodosLosActivos();
}
