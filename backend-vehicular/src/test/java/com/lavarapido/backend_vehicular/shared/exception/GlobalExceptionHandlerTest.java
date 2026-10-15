package com.lavarapido.backend_vehicular.shared.exception;

import org.junit.jupiter.api.Test;
import com.lavarapido.backend_vehicular.reservas.exception.ReservaCancelacionConflictException;
import com.lavarapido.backend_vehicular.users.exception.CurrentPasswordInvalidException;

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

    @Test
    void incorrectCurrentPasswordReturnsBadRequestWithStableCode() {
        var response = new GlobalExceptionHandler()
                .handleCurrentPasswordInvalid(new CurrentPasswordInvalidException());

        assertEquals(400, response.getStatusCode().value());
        assertEquals("CURRENT_PASSWORD_INVALID", response.getBody().get("error"));
    }

    @Test
    void reservationCancellationConflictReturnsStableMachineReadableCode() {
        var response = new GlobalExceptionHandler().handleReservationCancellationConflict(
                new ReservaCancelacionConflictException("PAGO_PENDIENTE", "No se puede confirmar"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("PAGO_PENDIENTE", response.getBody().get("code"));
        assertEquals("No se puede confirmar", response.getBody().get("error"));
    }
}
