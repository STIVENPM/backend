package com.lavarapido.backend_vehicular.calificaciones.repository;
import com.lavarapido.backend_vehicular.calificaciones.entity.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
/** Consulta las calificaciones por reserva y comprueba si una reserva ya fue calificada. */
public interface CalificacionRepository extends JpaRepository<Calificacion, UUID> {
    // Comprueba si la reserva ya recibió una calificación.
    boolean existsByReserva_IdReserva(UUID idReserva);
    // Permite mostrar la calificación asociada a la reserva.
    Optional<Calificacion> findByReserva_IdReserva(UUID idReserva);
}
