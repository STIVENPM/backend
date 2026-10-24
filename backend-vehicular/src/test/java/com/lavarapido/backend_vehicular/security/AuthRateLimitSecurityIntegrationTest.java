package com.lavarapido.backend_vehicular.security;

import com.lavarapido.backend_vehicular.shared.config.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.mockito.Mockito.mock;

class AuthRateLimitSecurityIntegrationTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;
    private TestLoginController loginController;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfiguration.class);
        context.refresh();
        loginController = context.getBean(TestLoginController.class);
        mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void sixthFailedLoginIsRateLimitedByTheSpringSecurityChain() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/users/login")
                            .with(remoteAddress("192.0.2.10")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/users/login")
                        .with(remoteAddress("192.0.2.10")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"));
    }

    @Test
    void successfulLoginClearsTheSpringSecurityChainWindow() throws Exception {
        for (int attempt = 0; attempt < 4; attempt++) {
            mockMvc.perform(post("/api/users/login")
                    .with(remoteAddress("192.0.2.11")))
                    .andExpect(status().isUnauthorized());
        }

        loginController.success = true;
        mockMvc.perform(post("/api/users/login")
                        .with(remoteAddress("192.0.2.11")))
                .andExpect(status().isOk());

        loginController.success = false;
        mockMvc.perform(post("/api/users/login")
                        .with(remoteAddress("192.0.2.11")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void countersAreSeparatedByClientAddressAndRoute() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/users/login")
                    .with(remoteAddress("192.0.2.12")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/users/login")
                        .with(remoteAddress("192.0.2.13")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .with(remoteAddress("192.0.2.12")))
                .andExpect(result -> assertEquals(404, result.getResponse().getStatus()));
    }

    private RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import(SecurityConfig.class)
    static class TestConfiguration {
        @Bean
        AuthRateLimitFilter authRateLimitFilter() {
            return new AuthRateLimitFilter();
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(mock(JwtService.class), mock(AccountAccessService.class));
        }

        @Bean
        TestLoginController testLoginController() {
            return new TestLoginController();
        }
    }

    @RestController
    @RequestMapping("/api/users")
    static class TestLoginController {
        private boolean success;

        @PostMapping("/login")
        ResponseEntity<Void> login() {
            return success ? ResponseEntity.ok().build() : ResponseEntity.status(401).build();
        }
    }
}
