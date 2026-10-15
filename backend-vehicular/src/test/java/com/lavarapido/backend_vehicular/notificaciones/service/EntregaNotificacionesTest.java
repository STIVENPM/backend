package com.lavarapido.backend_vehicular.notificaciones.service;

import com.lavarapido.backend_vehicular.notificaciones.entity.*;
import com.lavarapido.backend_vehicular.notificaciones.repository.*;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EntregaNotificacionesTest {
    @Test void payloadPushEsVisibleYUsaElCanalAndroidConfigurado() {
        var notification = new Notificacion();
        notification.id = UUID.randomUUID(); notification.titulo = "Reserva confirmada";
        notification.mensaje = "Tu reserva fue confirmada";
        var device = new DispositivoPush(); device.token = "ExpoPushToken[test]";

        var payload = EntregaNotificaciones.crearPayloadPush(notification, device);

        assertEquals(device.token, payload.get("to"));
        assertEquals(notification.titulo, payload.get("title"));
        assertEquals(notification.mensaje, payload.get("body"));
        assertEquals("general", payload.get("channelId"));
        assertEquals("high", payload.get("priority"));
        assertEquals(notification.id.toString(), ((java.util.Map<?, ?>) payload.get("data")).get("notificacionId"));
    }

    @Test void falloSmtpAgendaReintentoYWorkerLoRecupera() {
        var deliveries = mock(NotificacionEntregaRepository.class);
        var notifications = mock(NotificacionRepository.class);
        var devices = mock(DispositivoPushRepository.class);
        var users = mock(UserRepository.class);
        var mail = mock(JavaMailSender.class);
        var worker = new EntregaNotificaciones(deliveries, notifications, devices, users, mail);
        ReflectionTestUtils.setField(worker, "emailEnabled", true);
        var delivery = new NotificacionEntrega();
        delivery.id = UUID.randomUUID(); delivery.notificacionId = UUID.randomUUID(); delivery.canal = "email";
        delivery.estado = "pendiente"; delivery.proximaEjecucion = LocalDateTime.now();
        var notification = new Notificacion();
        notification.usuarioId = UUID.randomUUID(); notification.titulo = "Reserva"; notification.mensaje = "Creada";
        var user = new User(); user.setStatus(true); user.setEmail("owner@example.test");
        when(deliveries.reclamarPendientes()).thenReturn(List.of(delivery));
        when(notifications.findById(delivery.notificacionId)).thenReturn(Optional.of(notification));
        when(users.findById(notification.usuarioId)).thenReturn(Optional.of(user));
        doThrow(new MailSendException("smtp unavailable")).doNothing().when(mail).send(any(org.springframework.mail.SimpleMailMessage.class));
        worker.procesarPendientes();
        assertEquals("pendiente", delivery.estado);
        assertEquals(1, delivery.intentos);
        assertNotNull(delivery.ultimoError);
        assertTrue(delivery.proximaEjecucion.isAfter(LocalDateTime.now()));
        worker.procesarPendientes();
        assertEquals("aceptada", delivery.estado);
        verify(deliveries, times(2)).save(delivery);
    }
}
