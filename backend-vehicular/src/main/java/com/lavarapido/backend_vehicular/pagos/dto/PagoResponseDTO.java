package com.lavarapido.backend_vehicular.pagos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagoResponseDTO(
        UUID idPago, UUID idReserva, String referenciaPago, String metodoPago,
        BigDecimal monto, String estadoWompi, String estado, LocalDateTime fechaPago) { }
