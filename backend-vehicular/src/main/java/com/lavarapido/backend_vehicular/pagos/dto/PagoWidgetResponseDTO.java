package com.lavarapido.backend_vehicular.pagos.dto;

import java.util.UUID;

/** Entrega al frontend los datos necesarios para abrir el widget de Wompi; incluye llave pública y firma, no secretos privados. */
public record PagoWidgetResponseDTO(
        UUID idPago, UUID idIntento, UUID idReserva, String referencia, long montoEnCentavos,
        String moneda, String publicKey, String firmaIntegridad,
        String redirectUrl, boolean reutilizado) { }
