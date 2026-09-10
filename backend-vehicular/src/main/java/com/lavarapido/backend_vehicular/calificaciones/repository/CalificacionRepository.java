package com.lavarapido.backend_vehicular.calificaciones.repository;
import com.lavarapido.backend_vehicular.calificaciones.entity.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface CalificacionRepository extends JpaRepository<Calificacion, UUID> {
    boolean existsByReserva_IdReserva(UUID idReserva);
    Optional<Calificacion> findByReserva_IdReserva(UUID idReserva);
}
