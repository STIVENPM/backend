package com.lavarapido.backend_vehicular.auth.controller;

import com.lavarapido.backend_vehicular.auth.exception.EmailDeliveryException;
import com.lavarapido.backend_vehicular.auth.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PasswordResetControllerTest {
    private final PasswordResetService service = mock(PasswordResetService.class);
    private final PasswordResetController controller = new PasswordResetController(service);

    @Test
    void recoveryResponseDoesNotDiscloseAccountOrDeliveryState() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        var unknown = controller.forgotPassword(
                new PasswordResetController.ForgotPasswordRequest("unknown@example.test"), request);
        doThrow(new EmailDeliveryException("private mail detail", new RuntimeException("private")))
                .when(service).solicitarRecuperacion(anyString(), anyString());
        var deliveryFailed = controller.forgotPassword(
                new PasswordResetController.ForgotPasswordRequest("known@example.test"), request);

        assertEquals(200, unknown.getStatusCode().value());
        assertEquals(unknown.getBody(), deliveryFailed.getBody());
        assertEquals(200, deliveryFailed.getStatusCode().value());
    }
}
