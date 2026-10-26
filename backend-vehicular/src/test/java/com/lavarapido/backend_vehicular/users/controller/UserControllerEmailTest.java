package com.lavarapido.backend_vehicular.users.controller;

import com.lavarapido.backend_vehicular.shared.exception.GlobalExceptionHandler;
import com.lavarapido.backend_vehicular.users.dto.ChangeEmailRequestDTO;
import com.lavarapido.backend_vehicular.users.dto.UserProfileResponseDTO;
import com.lavarapido.backend_vehicular.users.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerEmailTest {
    private final UserService userService = mock(UserService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void changeEmailEndpointNormalizesEmailBeforeValidationAndServiceCall() throws Exception {
        when(userService.changeEmail(any(ChangeEmailRequestDTO.class))).thenReturn(
                new UserProfileResponseDTO(UUID.randomUUID(), "usuario@ejemplo.com",
                        "Ana", "Prueba", "3001234567", "avatar_1"));

        mockMvc.perform(put("/api/users/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "CurrentPass1",
                                  "newEmail": "  Usuario@Ejemplo.com  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("usuario@ejemplo.com"));

        verify(userService).changeEmail(new ChangeEmailRequestDTO(
                "CurrentPass1", "usuario@ejemplo.com"));
    }

    @Test
    void changeEmailEndpointRejectsInvalidEmailBeforeServiceCall() throws Exception {
        mockMvc.perform(put("/api/users/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "CurrentPass1",
                                  "newEmail": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(userService);
    }
}
