package com.lavarapido.backend_vehicular.reservas.controller;

import com.lavarapido.backend_vehicular.reservas.dto.ReservaResponseDTO;
import com.lavarapido.backend_vehicular.reservas.dto.ReservationCancellationRequest;
import com.lavarapido.backend_vehicular.reservas.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

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
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("192.0.2.10");
        when(reservaService.cancelWithResult(reservationId, null, "192.0.2.10"))
                .thenReturn(new ReservaService.ResultadoCancelacion(response, false));

        var result = controller.cancel(reservationId, null, httpRequest);

        assertEquals(200, result.getStatusCode().value());
        assertEquals("CANCELLED", result.getHeaders().getFirst("X-Reservation-Cancellation-Result"));
        assertEquals(response, result.getBody());
    }

    @Test
    void cancellationResponseIdentifiesAlreadyCancelledReservation() {
        ReservaResponseDTO response = new ReservaResponseDTO();
        UUID reservationId = UUID.randomUUID();
        ReservationCancellationRequest request = new ReservationCancellationRequest(true, "2026-10-26");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        when(reservaService.cancelWithResult(reservationId, request, "127.0.0.1"))
                .thenReturn(new ReservaService.ResultadoCancelacion(response, true));

        var result = controller.cancel(reservationId, request, httpRequest);

        assertEquals(200, result.getStatusCode().value());
        assertEquals("ALREADY_CANCELLED", result.getHeaders().getFirst("X-Reservation-Cancellation-Result"));
        assertEquals(response, result.getBody());
    }
}
