package com.lavarapido.backend_vehicular.reservas.repository;

import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;

/** Consulta reservas por usuario, estado o fecha y detecta cruces de horario por vehículo. */
public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    // Bloquea una reserva durante operaciones concurrentes, como iniciar un pago.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.idReserva = :idReserva")
    Optional<Reserva> findByIdForUpdate(@Param("idReserva") UUID idReserva);

    List<Reserva> findByUsuario_UserId(UUID usuarioId);

    List<Reserva> findByUsuario_UserIdOrderByFechaReservaDescHoraReservaDesc(UUID usuarioId);

    List<Reserva> findByEstado(EstadoReserva estado);

    List<Reserva> findByFechaReservaBetween(LocalDate fechaInicio, LocalDate fechaFin);

    List<Reserva> findByUsuario_UserIdAndFechaReservaBetween(UUID usuarioId, LocalDate fechaInicio, LocalDate fechaFin);

    // Detecta cruces de horario del mismo vehículo en la fecha solicitada.
    // No cuenta reservas canceladas; usa la duración pactada al reservar.
    @Query(value = """
            SELECT r.*
            FROM reservas r
            WHERE r.fk_id_vehiculo = :idVehiculo
              AND r.fecha_reserva = :fechaReserva
              AND r.estado <> :estadoCancelada
              AND r.hora_reserva < :horaFin
              AND :horaInicio < r.hora_reserva + (r.duracion_minutos_pactada * INTERVAL '1 minute')
            """, nativeQuery = true)
    List<Reserva> findSolapamientosPorVehiculo(
            @Param("idVehiculo") UUID idVehiculo,
            @Param("fechaReserva") LocalDate fechaReserva,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("estadoCancelada") String estadoCancelada
    );
}
