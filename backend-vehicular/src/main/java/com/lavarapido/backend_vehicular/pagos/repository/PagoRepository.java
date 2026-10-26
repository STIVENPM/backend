package com.lavarapido.backend_vehicular.pagos.repository;

import com.lavarapido.backend_vehicular.pagos.entity.Pago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

/** Consulta los pagos de las reservas y bloquea filas durante operaciones concurrentes. */
public interface PagoRepository extends JpaRepository<Pago, UUID> {
    // Cada reserva puede tener un pago con varios intentos asociados.
    Optional<Pago> findByReserva_IdReserva(UUID idReserva);

    // Serializa cambios simultáneos sobre el mismo pago.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.idPago = :idPago")
    Optional<Pago> findByIdForUpdate(@Param("idPago") UUID idPago);

    // Evita que dos solicitudes creen o modifiquen el pago de una reserva a la vez.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.reserva.idReserva = :idReserva")
    Optional<Pago> findByReservaIdForUpdate(@Param("idReserva") UUID idReserva);
}
