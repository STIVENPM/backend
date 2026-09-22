package com.lavarapido.backend_vehicular.pagos.dto;

import java.time.LocalDateTime;
import java.util.UUID;

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
