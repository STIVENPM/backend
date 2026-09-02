package com.lavarapido.backend_vehicular.reservas.repository;

import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    List<Reserva> findByUsuario_UserId(UUID usuarioId);

    List<Reserva> findByUsuario_UserIdOrderByFechaReservaDescHoraReservaDesc(UUID usuarioId);

    List<Reserva> findByEstado(EstadoReserva estado);

    List<Reserva> findByFechaReservaBetween(LocalDate fechaInicio, LocalDate fechaFin);

    List<Reserva> findByUsuario_UserIdAndFechaReservaBetween(UUID usuarioId, LocalDate fechaInicio, LocalDate fechaFin);

    @Query(value = """
            SELECT r.*
            FROM reservas r
            JOIN servicios s ON s.id_servicio = r.fk_id_servicio
            WHERE r.fk_id_vehiculo = :idVehiculo
              AND r.fecha_reserva = :fechaReserva
              AND r.estado <> :estadoCancelada
              AND r.hora_reserva < :horaFin
              AND :horaInicio < r.hora_reserva + (s.duracion_minutos * INTERVAL '1 minute')
            """, nativeQuery = true)
    List<Reserva> findSolapamientosPorVehiculo(
            @Param("idVehiculo") UUID idVehiculo,
            @Param("fechaReserva") LocalDate fechaReserva,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("estadoCancelada") String estadoCancelada
    );
}
