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

    @Query("""
            SELECT r FROM Reserva r
            WHERE r.vehiculo.idVehiculo = :idVehiculo
              AND r.fechaReserva = :fechaReserva
              AND r.estado <> :estadoCancelada
              AND r.horaReserva < :horaFin
              AND r.horaReserva > :horaInicio
            """)
    List<Reserva> findSolapamientosPorVehiculo(
            @Param("idVehiculo") UUID idVehiculo,
            @Param("fechaReserva") LocalDate fechaReserva,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("estadoCancelada") EstadoReserva estadoCancelada
    );
}
