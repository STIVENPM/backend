package com.lavarapido.backend_vehicular.pagos.service;

import tools.jackson.databind.JsonNode;

public interface WompiTransactionClient {
    JsonNode consultar(String transactionId);
}
