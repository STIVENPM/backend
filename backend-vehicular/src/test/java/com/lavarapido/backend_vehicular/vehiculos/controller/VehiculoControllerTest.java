package com.lavarapido.backend_vehicular.vehiculos.controller;

import com.lavarapido.backend_vehicular.shared.exception.GlobalExceptionHandler;
import com.lavarapido.backend_vehicular.vehiculos.exception.PlacaVehiculoInvalidaException;
import com.lavarapido.backend_vehicular.vehiculos.service.VehiculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VehiculoControllerTest {

    private final VehiculoService service = mock(VehiculoService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VehiculoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsBadRequestForInvalidPlateTypeCombination() throws Exception {
        doThrow(new PlacaVehiculoInvalidaException("Para MOTO, la placa debe tener el formato ABC12D"))
                .when(service).crear(any());

        mockMvc.perform(post("/api/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Para MOTO, la placa debe tener el formato ABC12D"));
    }

    @Test
    void returnsConflictForDuplicatePlateInsteadOfConvertingItToBadRequest() throws Exception {
        doThrow(new IllegalStateException("Ya existe un vehículo registrado con esa placa"))
                .when(service).crear(any());

        mockMvc.perform(post("/api/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("La operacion entra en conflicto con el estado actual"));
    }

    private String requestBody() {
        return """
                {
                  "placa": "ABC12D",
                  "color": "Negro",
                  "tipoVehiculo": "MOTO",
                  "fkIdMarca": "%s"
                }
                """.formatted(UUID.randomUUID());
    }
}
