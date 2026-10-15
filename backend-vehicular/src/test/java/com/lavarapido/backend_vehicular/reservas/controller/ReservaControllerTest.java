package com.lavarapido.backend_vehicular.reservas.controller;

import com.lavarapido.backend_vehicular.reservas.dto.ReservaResponseDTO;
import com.lavarapido.backend_vehicular.reservas.service.ReservaService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservaControllerTest {
    private final ReservaService reservaService = mock(ReservaService.class);
    private final ReservaController controller = new ReservaController(reservaService);

    @Test
    void cancellationResponseIdentifiesNewCancellation() {
        ReservaResponseDTO response = new ReservaResponseDTO();
        UUID reservationId = UUID.randomUUID();
        when(reservaService.cancelarConResultado(reservationId))
                .thenReturn(new ReservaService.ResultadoCancelacion(response, false));

        var result = controller.cancelar(reservationId);

        assertEquals(200, result.getStatusCode().value());
        assertEquals("CANCELLED", result.getHeaders().getFirst("X-Reservation-Cancellation-Result"));
        assertEquals(response, result.getBody());
    }

    @Test
    void cancellationResponseIdentifiesAlreadyCancelledReservation() {
        ReservaResponseDTO response = new ReservaResponseDTO();
        UUID reservationId = UUID.randomUUID();
        when(reservaService.cancelarConResultado(reservationId))
                .thenReturn(new ReservaService.ResultadoCancelacion(response, true));

        var result = controller.cancelar(reservationId);

        assertEquals(200, result.getStatusCode().value());
        assertEquals("ALREADY_CANCELLED", result.getHeaders().getFirst("X-Reservation-Cancellation-Result"));
        assertEquals(response, result.getBody());
    }
}
