package com.lavarapido.backend_vehicular.pagos.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Devuelve un intento de pago y su estado local y en Wompi para mostrar su seguimiento. */
public record PagoIntentoResponseDTO(
        UUID idIntento,
        String referencia,
        String wompiTransactionId,
        String wompiPaymentMethodType,
        String wompiStatus,
        String wompiEnvironment,
        String estado,
        LocalDateTime fechaConfirmacion,
        LocalDateTime createdAt
) { }
