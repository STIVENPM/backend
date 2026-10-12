package com.lavarapido.backend_vehicular.notificaciones.service;

import com.lavarapido.backend_vehicular.notificaciones.entity.Notificacion;
import com.lavarapido.backend_vehicular.notificaciones.entity.DispositivoPush;
import com.lavarapido.backend_vehicular.notificaciones.repository.*;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificacionServiceTest {
    private final NotificacionRepository notifications = mock(NotificacionRepository.class);
    private final NotificacionEntregaRepository deliveries = mock(NotificacionEntregaRepository.class);
    private final DispositivoPushRepository devices = mock(DispositivoPushRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AccountAccessService access = mock(AccountAccessService.class);
    private final NotificacionService service = new NotificacionService(notifications, deliveries, devices, users, access);
    private final User owner = new User();
    private final UUID id = UUID.randomUUID();

    @BeforeEach void auth() {
        owner.setUserId(id); owner.setStatus(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@example.test", null));
        when(access.activeUser("owner@example.test")).thenReturn(owner);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void listaYCuentaSoloUsuarioAutenticado() {
        when(notifications.findByUsuarioIdOrderByCreatedAtDesc(eq(id), any())).thenReturn(Page.empty());
        when(notifications.countByUsuarioIdAndLeidaAtIsNull(id)).thenReturn(3L);
        assertEquals(0, service.listar(0, 10, false).getTotalElements());
        assertEquals(3, service.noLeidas());
        verify(notifications).findByUsuarioIdOrderByCreatedAtDesc(eq(id), any());
        verify(notifications).countByUsuarioIdAndLeidaAtIsNull(id);
        assertThrows(IllegalArgumentException.class, () -> service.listar(0, 51, false));
    }

    @Test void noPermiteMarcarNotificacionAjena() {
        UUID ajena = UUID.randomUUID();
        when(notifications.findByIdAndUsuarioId(ajena, id)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.marcarLeida(ajena));
        verify(notifications, never()).save(any());
    }

    @Test void leerEsIdempotente() {
        Notificacion n = new Notificacion(); n.id = UUID.randomUUID(); n.usuarioId = id;
        when(notifications.findByIdAndUsuarioId(n.id, id)).thenReturn(Optional.of(n));
        when(notifications.save(n)).thenReturn(n);
        var first = service.marcarLeida(n.id);
        var second = service.marcarLeida(n.id);
        assertEquals(first.leidaAt(), second.leidaAt());
    }

    @Test void noDuplicaEventoNiEntrega() {
        when(notifications.existsByUsuarioIdAndClaveEvento(id, "evento:1")).thenReturn(true);
        service.crear(owner, "evento:1", "booking", "Reserva", "Creada", UUID.randomUUID(), null);
        verify(notifications, never()).save(any());
        verify(deliveries, never()).save(any());
    }

    @Test void creaNotificacionYCorreoPendiente() {
        when(notifications.existsByUsuarioIdAndClaveEvento(id, "evento:2")).thenReturn(false);
        when(notifications.save(any())).thenAnswer(invocation -> {
            Notificacion n = invocation.getArgument(0); n.id = UUID.randomUUID(); return n;
        });
        when(devices.findByUsuarioIdAndActivoTrue(id)).thenReturn(List.of());
        service.crear(owner, "evento:2", "booking", "Reserva", "Creada", UUID.randomUUID(), null);
        verify(notifications).save(any());
        verify(deliveries).save(argThat(e -> "email".equals(e.canal) && e.notificacionId != null));
    }

    @Test void rotaYDesactivaElDispositivoDeLaCuentaActual() {
        DispositivoPush device = new DispositivoPush();
        device.id = UUID.randomUUID(); device.usuarioId = id;
        device.instalacionId = "install-123"; device.token = "ExpoPushToken[old]";
        when(devices.findByToken("ExpoPushToken[new]")).thenReturn(Optional.empty());
        when(devices.findByUsuarioIdAndInstalacionId(id, "install-123")).thenReturn(Optional.of(device));
        service.registrarDispositivo("install-123", "ExpoPushToken[new]");
        assertEquals("ExpoPushToken[new]", device.token);
        assertTrue(device.activo);
        service.desactivarDispositivo("install-123");
        assertFalse(device.activo);
        verify(devices, times(2)).save(device);
    }

    @Test void transferenciaDeTokenRevocaAsociacionAnterior() {
        DispositivoPush anterior = new DispositivoPush();
        anterior.id = UUID.randomUUID(); anterior.usuarioId = UUID.randomUUID();
        anterior.instalacionId = "install-old"; anterior.token = "ExpoPushToken[same]"; anterior.activo = true;
        when(devices.findByToken("ExpoPushToken[same]")).thenReturn(Optional.of(anterior));
        when(devices.findByUsuarioIdAndInstalacionId(id, "install-new")).thenReturn(Optional.empty());
        service.registrarDispositivo("install-new", "ExpoPushToken[same]");
        assertFalse(anterior.activo);
        assertEquals("revoked-" + anterior.id, anterior.token);
        verify(devices).saveAndFlush(anterior);
        verify(devices).save(argThat(d -> d.usuarioId.equals(id) && d.activo));
    }

    @Test void alertaAdministrativaSoloSeCreaParaAdministradoresActivos() {
        User operador = new User(); operador.setUserId(UUID.randomUUID()); operador.setStatus(true);
        User admin = new User(); admin.setUserId(UUID.randomUUID()); admin.setStatus(true);
        when(users.findActiveAdministratorCandidates()).thenReturn(List.of(operador, admin));
        when(access.currentRole(operador)).thenReturn("OPERATOR");
        when(access.currentRole(admin)).thenReturn("ADMIN");
        when(notifications.save(any())).thenAnswer(invocation -> {
            Notificacion n = invocation.getArgument(0); n.id = UUID.randomUUID(); return n;
        });
        when(devices.findByUsuarioIdAndActivoTrue(admin.getUserId())).thenReturn(List.of());

        service.alertarAdministradores("alerta:1", "Alerta", "Revisar", UUID.randomUUID(), null);

        verify(notifications).existsByUsuarioIdAndClaveEvento(admin.getUserId(), "alerta:1");
        verify(notifications, never()).existsByUsuarioIdAndClaveEvento(eq(operador.getUserId()), any());
        verify(notifications).save(argThat(n -> n.usuarioId.equals(admin.getUserId())));
    }
}
