package com.lavarapido.backend_vehicular.pagos.repository;

import com.lavarapido.backend_vehicular.pagos.entity.PagoIntento;
import com.lavarapido.backend_vehicular.pagos.enums.EstadoIntentoPago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PagoIntentoRepository extends JpaRepository<PagoIntento, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PagoIntento i join fetch i.pago p join fetch p.reserva where i.referencia = :referencia")
    Optional<PagoIntento> findByReferenciaForUpdate(@Param("referencia") String referencia);

    Optional<PagoIntento> findFirstByPago_IdPagoAndEstadoOrderByCreatedAtDesc(
            UUID idPago, EstadoIntentoPago estado);

    Optional<PagoIntento> findByWompiEnvironmentAndWompiTransactionId(
            String wompiEnvironment, String wompiTransactionId);

    List<PagoIntento> findByPago_IdPagoOrderByCreatedAtAsc(UUID idPago);

    List<PagoIntento> findByPago_IdPagoAndEstadoOrderByCreatedAtAsc(
            UUID idPago, EstadoIntentoPago estado);
}
