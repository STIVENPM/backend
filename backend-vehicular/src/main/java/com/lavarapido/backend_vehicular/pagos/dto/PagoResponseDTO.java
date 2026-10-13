package com.lavarapido.backend_vehicular.pagos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

/** Devuelve el pago de una reserva, su intento actual y el historial de intentos. */
public record PagoResponseDTO(
        UUID idPago,
        UUID idReserva,
        String metodoPago,
        BigDecimal monto,
        String estado,
        LocalDateTime fechaPago,
        PagoIntentoResponseDTO intentoActual,
        List<PagoIntentoResponseDTO> intentos
) { }
