package com.lavarapido.backend_vehicular.pagos.dto;

import java.util.UUID;

public record PagoWidgetResponseDTO(
        UUID idPago, UUID idIntento, UUID idReserva, String referencia, long montoEnCentavos,
        String moneda, String publicKey, String firmaIntegridad,
        String redirectUrl, boolean reutilizado) { }
