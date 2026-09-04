package com.lavarapido.backend_vehicular.pagos.repository;

import com.lavarapido.backend_vehicular.pagos.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PagoRepository extends JpaRepository<Pago, UUID> {
    boolean existsByReserva_IdReserva(UUID idReserva);
    Optional<Pago> findByReserva_IdReserva(UUID idReserva);
    Optional<Pago> findByReferenciaPago(String referenciaPago);
}
