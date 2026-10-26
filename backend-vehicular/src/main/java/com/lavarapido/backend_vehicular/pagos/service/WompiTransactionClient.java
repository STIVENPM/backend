package com.lavarapido.backend_vehicular.pagos.service;

import tools.jackson.databind.JsonNode;

// Abstrae la consulta a Wompi para poder probar pagos sin llamadas reales.
// PagoService depende de este contrato, no del cliente HTTP concreto.
public interface WompiTransactionClient {
    JsonNode consultar(String transactionId);
}
