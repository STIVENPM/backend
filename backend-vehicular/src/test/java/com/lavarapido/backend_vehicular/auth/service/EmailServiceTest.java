package com.lavarapido.backend_vehicular.auth.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceTest {

    @Test
    void buildsTheFrontendResetRouteWithTheTokenAndAuthorizedSender() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, "https://app.example.test/", "noreply@example.test");
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);

        service.enviarCorreoRecuperacion("user@example.test", "safe-token");

        verify(sender).send(message.capture());
        assertEquals("noreply@example.test", message.getValue().getFrom());
        assertEquals("user@example.test", message.getValue().getTo()[0]);
        assertTrue(message.getValue().getText().contains("https://app.example.test/reset-password?token=safe-token"));
    }
}
