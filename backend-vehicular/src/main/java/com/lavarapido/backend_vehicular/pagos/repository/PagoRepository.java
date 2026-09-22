package com.lavarapido.backend_vehicular.pagos.repository;

import com.lavarapido.backend_vehicular.pagos.entity.Pago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface PagoRepository extends JpaRepository<Pago, UUID> {
    Optional<Pago> findByReserva_IdReserva(UUID idReserva);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.idPago = :idPago")
    Optional<Pago> findByIdForUpdate(@Param("idPago") UUID idPago);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.reserva.idReserva = :idReserva")
    Optional<Pago> findByReservaIdForUpdate(@Param("idReserva") UUID idReserva);
}
