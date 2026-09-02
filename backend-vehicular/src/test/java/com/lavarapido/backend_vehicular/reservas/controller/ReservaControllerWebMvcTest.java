package com.lavarapido.backend_vehicular.reservas.controller;

import com.lavarapido.backend_vehicular.reservas.service.ReservaService;
import com.lavarapido.backend_vehicular.security.JwtAuthenticationFilter;
import com.lavarapido.backend_vehicular.security.JwtService;
import com.lavarapido.backend_vehicular.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ReservaControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ReservaService reservaService;
    @MockitoBean private JwtService jwtService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtenerTodas_adminPuedeAcceder() throws Exception {
        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk());

        verify(reservaService).obtenerTodas();
    }

    @Test
    @WithMockUser(roles = "USER")
    void obtenerTodas_userNoPuedeAcceder() throws Exception {
        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void crear_userAutenticadoPuedeAcceder() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fkIdVehiculo": "37ee5630-748c-4d96-af2a-7dd6cd2fc097",
                                  "fkIdServicio": "2c0a099b-f3f3-4d68-a4f0-ed8623d7c88f",
                                  "fechaReserva": "2026-09-10",
                                  "horaReserva": "10:00:00"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(reservaService).crear(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cambiarEstado_adminPuedeAcceder() throws Exception {
        mockMvc.perform(patch("/api/reservas/b6a7ebd5-a2f1-49fe-9c12-8c68842d52e8/estado")
                        .param("estado", "ASIGNADA"))
                .andExpect(status().isOk());

        verify(reservaService).cambiarEstado(
                java.util.UUID.fromString("b6a7ebd5-a2f1-49fe-9c12-8c68842d52e8"),
                com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva.ASIGNADA);
    }

    @Test
    @WithMockUser(roles = "USER")
    void cancelar_userAutenticadoPuedeLlegarAlServicio() throws Exception {
        mockMvc.perform(patch("/api/reservas/b6a7ebd5-a2f1-49fe-9c12-8c68842d52e8/cancelar"))
                .andExpect(status().isOk());

        verify(reservaService).cancelar(
                java.util.UUID.fromString("b6a7ebd5-a2f1-49fe-9c12-8c68842d52e8"));
    }
}
