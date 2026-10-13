package com.lavarapido.backend_vehicular.asignaciones.repository;

import com.lavarapido.backend_vehicular.asignaciones.entity.Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Accede a las asignaciones entre reservas y operadores; JpaRepository aporta las operaciones básicas de persistencia. */
public interface AsignacionRepository extends JpaRepository<Asignacion, UUID> {

    // Una reserva solo se asigna si todavía no tiene operador vinculado.
    boolean existsByReserva_IdReserva(UUID idReserva);

    // Recupera la asignación de una reserva concreta.
    Optional<Asignacion> findByReserva_IdReserva(UUID idReserva);

    // Obtiene asignaciones de varias reservas en una sola consulta.
    List<Asignacion> findByReserva_IdReservaIn(List<UUID> idReservas);

    // Muestra primero las asignaciones más recientes de un operador.
    List<Asignacion> findByOperador_IdOperadorOrderByFechaAsignacionDesc(UUID idOperador);
}
