package com.lavarapido.backend_vehicular.shared.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {
    @Test
    void internalExceptionReturnsGenericServerError() {
        var response = new GlobalExceptionHandler().handleUnexpected(
                new RuntimeException("internal-database-detail"));

        assertEquals(500, response.getStatusCode().value());
        assertFalse(response.getBody().toString().contains("internal-database-detail"));
    }
}
