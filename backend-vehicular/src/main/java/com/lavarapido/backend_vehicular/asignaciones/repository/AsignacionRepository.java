package com.lavarapido.backend_vehicular.asignaciones.repository;

import com.lavarapido.backend_vehicular.asignaciones.entity.Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AsignacionRepository extends JpaRepository<Asignacion, UUID> {

    boolean existsByReserva_IdReserva(UUID idReserva);

    Optional<Asignacion> findByReserva_IdReserva(UUID idReserva);

    List<Asignacion> findByOperador_IdOperadorOrderByFechaAsignacionDesc(UUID idOperador);
}
