package com.lavarapido.backend_vehicular.security;

import com.lavarapido.backend_vehicular.shared.config.SecurityConfig;
import com.lavarapido.backend_vehicular.users.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class AuthorizationSecurityIntegrationTest {
    private final JwtService jwtService = mock(JwtService.class);
    private final AccountAccessService accountAccessService = mock(AccountAccessService.class);
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.addBeanFactoryPostProcessor(beanFactory -> {
            beanFactory.registerSingleton("jwtService", jwtService);
            beanFactory.registerSingleton("accountAccessService", accountAccessService);
        });
        context.register(TestConfiguration.class);
        context.refresh();
        mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void anonymousUserCannotAccessAdministrativeEndpoint() throws Exception {
        mockMvc.perform(get("/api/operadores"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientCannotAccessAdministrativeEndpoint() throws Exception {
        authenticateAs("USER");

        mockMvc.perform(get("/api/operadores").header("Authorization", "Bearer signed-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorCannotAccessAdministrativeEndpoint() throws Exception {
        authenticateAs("OPERATOR");

        mockMvc.perform(get("/api/operadores").header("Authorization", "Bearer signed-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanAccessAdministrativeEndpoint() throws Exception {
        authenticateAs("ADMIN");

        mockMvc.perform(get("/api/operadores").header("Authorization", "Bearer signed-token"))
                .andExpect(status().isOk());
    }

    @Test
    void clientCanReadActiveBrandsButCannotListAllBrands() throws Exception {
        authenticateAs("USER");

        mockMvc.perform(get("/api/marcas/activas").header("Authorization", "Bearer signed-token"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/marcas").header("Authorization", "Bearer signed-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorCanUseOperatorAssignmentEndpointButNotCreateAssignments() throws Exception {
        authenticateAs("OPERATOR");

        mockMvc.perform(get("/api/asignaciones/mis-asignaciones")
                        .header("Authorization", "Bearer signed-token"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/asignaciones")
                        .header("Authorization", "Bearer signed-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanCreateAssignmentsButCannotUseOperatorOnlyEndpoint() throws Exception {
        authenticateAs("ADMIN");

        mockMvc.perform(post("/api/asignaciones")
                        .header("Authorization", "Bearer signed-token"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/asignaciones/mis-asignaciones")
                        .header("Authorization", "Bearer signed-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void paymentWebhookRemainsPublic() throws Exception {
        mockMvc.perform(post("/api/pagos/webhook"))
                .andExpect(status().isOk());
    }

    @Test
    void onlyCustomerRoleCanReachVoluntaryCancellationEndpoint() throws Exception {
        UUID reservationId = UUID.randomUUID();

        authenticateAs("USER");
        mockMvc.perform(patch("/api/reservas/{id}/cancelar", reservationId)
                        .header("Authorization", "Bearer signed-token"))
                .andExpect(status().isOk());

        authenticateAs("ADMIN");
        mockMvc.perform(patch("/api/reservas/{id}/cancelar", reservationId)
                        .header("Authorization", "Bearer signed-token"))
                .andExpect(status().isForbidden());

        authenticateAs("OPERATOR");
        mockMvc.perform(patch("/api/reservas/{id}/cancelar", reservationId)
                        .header("Authorization", "Bearer signed-token"))
                .andExpect(status().isForbidden());
    }

    private void authenticateAs(String role) {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        user.setEmail(role.toLowerCase() + "@example.test");
        user.setStatus(true);

        when(jwtService.isTokenValid("signed-token")).thenReturn(true);
        when(jwtService.extractUserId("signed-token")).thenReturn(userId);
        when(jwtService.extractEmail("signed-token")).thenReturn(user.getEmail());
        when(accountAccessService.activeUser(userId)).thenReturn(user);
        when(accountAccessService.currentRole(user)).thenReturn(role);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, ProtectedEndpointController.class})
    static class TestConfiguration {
        @Bean
        AuthRateLimitFilter authRateLimitFilter() {
            return new AuthRateLimitFilter();
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                JwtService jwtService,
                AccountAccessService accountAccessService
        ) {
            return new JwtAuthenticationFilter(jwtService, accountAccessService);
        }
    }

    @RestController
    static class ProtectedEndpointController {
        @GetMapping("/api/operadores")
        ResponseEntity<Void> operators() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/marcas/activas")
        ResponseEntity<Void> activeBrands() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/marcas")
        ResponseEntity<Void> allBrands() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/asignaciones/mis-asignaciones")
        ResponseEntity<Void> ownAssignments() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/asignaciones")
        ResponseEntity<Void> createAssignment() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/pagos/webhook")
        ResponseEntity<Void> paymentWebhook() {
            return ResponseEntity.ok().build();
        }

        @PatchMapping("/api/reservas/{id}/cancelar")
        ResponseEntity<Void> cancelReservation(@PathVariable UUID id) {
            return ResponseEntity.ok().build();
        }
    }
}
