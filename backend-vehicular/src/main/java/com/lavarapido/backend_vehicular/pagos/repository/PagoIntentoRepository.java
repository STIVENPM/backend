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

/** Consulta los intentos de pago por referencia, transacción de Wompi, estado o historial. */
public interface PagoIntentoRepository extends JpaRepository<PagoIntento, UUID> {
    Optional<PagoIntento> findByReferencia(String referencia);

    // La referencia identifica el intento dentro de un pago concreto.
    Optional<PagoIntento> findByReferenciaAndPago_IdPago(String referencia, UUID idPago);

    // Bloquea el intento y carga pago y reserva antes de procesar un evento Wompi.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PagoIntento i join fetch i.pago p join fetch p.reserva where i.referencia = :referencia")
    Optional<PagoIntento> findByReferenciaForUpdate(@Param("referencia") String referencia);

    // Encuentra el intento más reciente de un pago con el estado solicitado.
    Optional<PagoIntento> findFirstByPago_IdPagoAndEstadoOrderByCreatedAtDesc(
            UUID idPago, EstadoIntentoPago estado);

    // Evita confundir IDs de transacción entre ambientes de Wompi.
    Optional<PagoIntento> findByWompiEnvironmentAndWompiTransactionId(
            String wompiEnvironment, String wompiTransactionId);

    // Devuelve el historial completo del pago desde el intento más antiguo.
    List<PagoIntento> findByPago_IdPagoOrderByCreatedAtAsc(UUID idPago);

    List<PagoIntento> findByPago_IdPagoAndEstadoOrderByCreatedAtAsc(
            UUID idPago, EstadoIntentoPago estado);
}
