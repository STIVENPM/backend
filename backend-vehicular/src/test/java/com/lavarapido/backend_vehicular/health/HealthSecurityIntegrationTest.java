package com.lavarapido.backend_vehicular.health;

import com.lavarapido.backend_vehicular.health.controller.HealthController;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.security.AuthRateLimitFilter;
import com.lavarapido.backend_vehicular.security.JwtAuthenticationFilter;
import com.lavarapido.backend_vehicular.security.JwtService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class HealthSecurityIntegrationTest {
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
    void healthCheckIsPublicAndReturnsUp() throws Exception {
        mockMvc.perform(get("/healthz"))
                .andExpect(status().isOk());
    }

    @Test
    void healthCheckOnlyAllowsGet() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/healthz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validJwtCanAccessProtectedEndpoint() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        user.setEmail("user@example.test");

        when(jwtService.isTokenValid("signed-token")).thenReturn(true);
        when(jwtService.extractUserId("signed-token")).thenReturn(userId);
        when(jwtService.extractEmail("signed-token")).thenReturn(user.getEmail());
        when(accountAccessService.activeUser(userId)).thenReturn(user);
        when(accountAccessService.currentRole(user)).thenReturn("USER");

        mockMvc.perform(get("/api/protected").header("Authorization", "Bearer signed-token"))
                .andExpect(status().isOk());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, HealthController.class, ProtectedController.class})
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
    static class ProtectedController {
        @GetMapping("/api/protected")
        ResponseEntity<Void> protectedEndpoint() {
            return ResponseEntity.ok().build();
        }
    }
}
