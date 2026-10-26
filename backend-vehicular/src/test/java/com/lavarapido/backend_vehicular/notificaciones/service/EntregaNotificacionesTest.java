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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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

    @Test
    void pushExitosoQuedaAceptadoHastaConfirmarRecibo() {
        var deliveries = mock(NotificacionEntregaRepository.class);
        var notifications = mock(NotificacionRepository.class);
        var devices = mock(DispositivoPushRepository.class);
        var users = mock(UserRepository.class);
        var mail = mock(JavaMailSender.class);
        var serverSetup = pushServer();
        var worker = new EntregaNotificaciones(
                deliveries, notifications, devices, users, mail, serverSetup.client());
        ReflectionTestUtils.setField(worker, "pushEnabled", true);

        var delivery = pushDelivery();
        var notification = notification(delivery.notificacionId);
        var device = device(delivery.dispositivoId, notification.usuarioId);
        when(deliveries.reclamarPendientes()).thenReturn(List.of(delivery));
        when(deliveries.reclamarRecibos()).thenReturn(List.of());
        when(notifications.findById(delivery.notificacionId)).thenReturn(Optional.of(notification));
        when(devices.findById(delivery.dispositivoId)).thenReturn(Optional.of(device));
        serverSetup.server().expect(requestTo("https://exp.host/--/api/v2/push/send"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"data\":{\"status\":\"ok\",\"id\":\"ticket-1\"}}",
                        MediaType.APPLICATION_JSON));

        worker.procesarPendientes();

        assertEquals("aceptada", delivery.estado);
        assertEquals("ticket-1", delivery.proveedorId);
        assertNotNull(delivery.enviadaAt);
        assertTrue(delivery.proximaEjecucion.isAfter(delivery.enviadaAt));
        serverSetup.server().verify();
    }

    @Test
    void falloDelProveedorAgendaReintentoSinRegistrarToken() {
        var deliveries = mock(NotificacionEntregaRepository.class);
        var notifications = mock(NotificacionRepository.class);
        var devices = mock(DispositivoPushRepository.class);
        var users = mock(UserRepository.class);
        var mail = mock(JavaMailSender.class);
        var serverSetup = pushServer();
        var worker = new EntregaNotificaciones(
                deliveries, notifications, devices, users, mail, serverSetup.client());
        ReflectionTestUtils.setField(worker, "pushEnabled", true);

        var delivery = pushDelivery();
        var notification = notification(delivery.notificacionId);
        var device = device(delivery.dispositivoId, notification.usuarioId);
        when(deliveries.reclamarPendientes()).thenReturn(List.of(delivery));
        when(deliveries.reclamarRecibos()).thenReturn(List.of());
        when(notifications.findById(delivery.notificacionId)).thenReturn(Optional.of(notification));
        when(devices.findById(delivery.dispositivoId)).thenReturn(Optional.of(device));
        serverSetup.server().expect(requestTo("https://exp.host/--/api/v2/push/send"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        worker.procesarPendientes();

        assertEquals("pendiente", delivery.estado);
        assertEquals(1, delivery.intentos);
        assertNotNull(delivery.ultimoError);
        assertFalse(delivery.ultimoError.contains(device.token));
        assertTrue(delivery.proximaEjecucion.isAfter(LocalDateTime.now()));
        serverSetup.server().verify();
    }

    @Test
    void reciboExpoConfirmaEntregaLuegoDeLaAceptacion() {
        var deliveries = mock(NotificacionEntregaRepository.class);
        var notifications = mock(NotificacionRepository.class);
        var devices = mock(DispositivoPushRepository.class);
        var users = mock(UserRepository.class);
        var mail = mock(JavaMailSender.class);
        var serverSetup = pushServer();
        var worker = new EntregaNotificaciones(
                deliveries, notifications, devices, users, mail, serverSetup.client());
        ReflectionTestUtils.setField(worker, "pushEnabled", true);

        var delivery = pushDelivery();
        delivery.estado = "aceptada";
        delivery.proveedorId = "ticket-1";
        when(deliveries.reclamarPendientes()).thenReturn(List.of());
        when(deliveries.reclamarRecibos()).thenReturn(List.of(delivery));
        serverSetup.server().expect(requestTo("https://exp.host/--/api/v2/push/getReceipts"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"data\":{\"ticket-1\":{\"status\":\"ok\"}}}",
                        MediaType.APPLICATION_JSON));

        worker.procesarPendientes();

        assertEquals("confirmada", delivery.estado);
        assertNull(delivery.ultimoError);
        serverSetup.server().verify();
    }

    private PushServer pushServer() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://exp.host");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new PushServer(builder.build(), server);
    }

    private NotificacionEntrega pushDelivery() {
        var delivery = new NotificacionEntrega();
        delivery.id = UUID.randomUUID();
        delivery.notificacionId = UUID.randomUUID();
        delivery.dispositivoId = UUID.randomUUID();
        delivery.canal = "push";
        delivery.estado = "pendiente";
        delivery.proximaEjecucion = LocalDateTime.now();
        return delivery;
    }

    private Notificacion notification(UUID notificationId) {
        var notification = new Notificacion();
        notification.id = notificationId;
        notification.usuarioId = UUID.randomUUID();
        notification.titulo = "Reserva";
        notification.mensaje = "Actualizada";
        return notification;
    }

    private DispositivoPush device(UUID deviceId, UUID userId) {
        var device = new DispositivoPush();
        device.id = deviceId;
        device.usuarioId = userId;
        device.token = "ExpoPushToken[test-token]";
        device.activo = true;
        return device;
    }

    private record PushServer(RestClient client, MockRestServiceServer server) { }
}
