package com.lavarapido.backend_vehicular.servicios.controller;

import com.lavarapido.backend_vehicular.servicios.dto.ServicioRequestDTO;
import com.lavarapido.backend_vehicular.servicios.dto.ServicioResponseDTO;
import com.lavarapido.backend_vehicular.servicios.service.ServicioService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicioControllerTest {
    private final ServicioService service = mock(ServicioService.class);
    private final ServicioController controller = new ServicioController(service);

    @Test
    void createsValidServiceWithCreatedStatus() {
        var request = new ServicioRequestDTO(
                "Lavado Premium", "sasasasasa", new BigDecimal("500005"), 27);
        when(service.crear(any(ServicioRequestDTO.class))).thenReturn(
                new ServicioResponseDTO(null, "Lavado Premium", "sasasasasa",
                        new BigDecimal("500005"), 27, true, null, null));

        var response = controller.crear(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("Lavado Premium", response.getBody().getNombre());
    }

    @Test
    void listsAllServicesWithTheirCurrentState() {
        when(service.listarTodos()).thenReturn(List.of(
                response("Active service", true), response("Inactive service", false)));

        var result = controller.listarTodos();

        assertEquals(200, result.getStatusCode().value());
        assertEquals(2, result.getBody().size());
        assertEquals(false, result.getBody().get(1).getEstado());
    }

    @Test
    void changesServiceStateAndReturnsUpdatedService() {
        UUID serviceId = UUID.randomUUID();
        when(service.cambiarEstado(serviceId, true)).thenReturn(
                new ServicioResponseDTO(serviceId, "Inactive service", "Description",
                        new BigDecimal("500005"), 27, true, null, null));

        var result = controller.cambiarEstado(serviceId, true);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(true, result.getBody().getEstado());
    }

    private ServicioResponseDTO response(String name, boolean active) {
        return new ServicioResponseDTO(UUID.randomUUID(), name, "Description",
                new BigDecimal("500005"), 27, active, null, null);
    }
}
