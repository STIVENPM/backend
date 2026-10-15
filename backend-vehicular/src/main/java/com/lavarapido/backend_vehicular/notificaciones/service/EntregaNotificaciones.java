package com.lavarapido.backend_vehicular.notificaciones.service;

import com.lavarapido.backend_vehicular.notificaciones.entity.*;
import com.lavarapido.backend_vehicular.notificaciones.repository.*;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class EntregaNotificaciones {
    private static final Logger log = LoggerFactory.getLogger(EntregaNotificaciones.class);
    private final NotificacionEntregaRepository entregas;
    private final NotificacionRepository notificaciones;
    private final DispositivoPushRepository dispositivos;
    private final UserRepository usuarios;
    private final JavaMailSender mail;
    private final RestClient push = crearClientePush();
    @Value("${app.notificaciones.email.enabled:false}") private boolean emailEnabled;
    @Value("${app.notificaciones.push.enabled:false}") private boolean pushEnabled;

    public EntregaNotificaciones(NotificacionEntregaRepository entregas, NotificacionRepository notificaciones,
            DispositivoPushRepository dispositivos, UserRepository usuarios, JavaMailSender mail) {
        this.entregas = entregas; this.notificaciones = notificaciones;
        this.dispositivos = dispositivos; this.usuarios = usuarios; this.mail = mail;
    }

    @PostConstruct
    void registrarConfiguracion() {
        if (pushEnabled) log.info("Entrega de notificaciones push habilitada");
        else log.warn("Entrega de notificaciones push deshabilitada; configure APP_NOTIFICACIONES_PUSH_ENABLED=true");
    }

    static Map<String, Object> crearPayloadPush(Notificacion notificacion, DispositivoPush dispositivo) {
        return Map.of(
                "to", dispositivo.token,
                "title", notificacion.titulo,
                "body", notificacion.mensaje,
                "channelId", "general",
                "priority", "high",
                "data", Map.of("notificacionId", notificacion.id.toString()));
    }

    private static RestClient crearClientePush() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl("https://exp.host").requestFactory(factory).build();
    }

    @Scheduled(fixedDelayString = "${app.notificaciones.worker.delay-ms:30000}")
    @Transactional
    public void procesarPendientes() {
        for (NotificacionEntrega entrega : entregas.reclamarPendientes()) {
            try {
                Notificacion n = notificaciones.findById(entrega.notificacionId).orElseThrow();
                if ("email".equals(entrega.canal)) {
                    if (!emailEnabled) continue;
                    var usuario = usuarios.findById(n.usuarioId).orElseThrow();
                    if (!Boolean.TRUE.equals(usuario.getStatus())) { entrega.estado = "fallida"; entrega.ultimoError = "Cuenta inactiva"; entregas.save(entrega); continue; }
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(usuario.getEmail()); message.setSubject(n.titulo); message.setText(n.mensaje);
                    mail.send(message);
                } else {
                    if (!pushEnabled) continue;
                    DispositivoPush d = dispositivos.findById(entrega.dispositivoId).orElseThrow();
                    if (!d.activo || !d.usuarioId.equals(n.usuarioId)) { entrega.estado = "fallida"; entrega.ultimoError = "Dispositivo inactivo"; entregas.save(entrega); continue; }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> response = push.post().uri("/--/api/v2/push/send")
                            .body(crearPayloadPush(n, d))
                            .retrieve().body(Map.class);
                    Object data = response == null ? null : response.get("data");
                    if (data instanceof java.util.List<?> list && !list.isEmpty()) data = list.get(0);
                    if (!(data instanceof Map<?, ?> ticket) || !"ok".equals(ticket.get("status"))) {
                        Object details = data instanceof Map<?, ?> failedTicket ? failedTicket.get("details") : null;
                        if (details instanceof Map<?, ?> info && "DeviceNotRegistered".equals(info.get("error"))) {
                            d.activo = false; dispositivos.save(d); entrega.estado = "fallida";
                            entrega.ultimoError = "DeviceNotRegistered"; entregas.save(entrega); continue;
                        }
                        throw new IllegalStateException("Proveedor push rechazó la solicitud");
                    }
                    Object ticketId = ticket.get("id");
                    if (!(ticketId instanceof String id) || id.isBlank()) throw new IllegalStateException("Ticket push inválido");
                    entrega.proveedorId = id;
                    entrega.estado = "aceptada";
                    entrega.proximaEjecucion = LocalDateTime.now().plusMinutes(15);
                }
                if ("email".equals(entrega.canal)) entrega.estado = "aceptada";
                entrega.enviadaAt = LocalDateTime.now();
                entrega.ultimoError = null;
            } catch (RuntimeException ex) {
                entrega.intentos++;
                entrega.ultimoError = ex.getClass().getSimpleName();
                if (entrega.intentos >= 5) entrega.estado = "fallida";
                else entrega.proximaEjecucion = LocalDateTime.now().plusMinutes(1L << entrega.intentos);
                log.warn("Entrega de notificación falló: id={}, canal={}, intento={}",
                        entrega.id, entrega.canal, entrega.intentos);
            }
            entregas.save(entrega);
        }
        if (pushEnabled) procesarRecibos();
    }

    private void procesarRecibos() {
        for (NotificacionEntrega entrega : entregas.reclamarRecibos()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = push.post().uri("/--/api/v2/push/getReceipts")
                        .body(Map.of("ids", java.util.List.of(entrega.proveedorId)))
                        .retrieve().body(Map.class);
                Object data = response == null ? null : response.get("data");
                Object receipt = data instanceof Map<?, ?> receipts ? receipts.get(entrega.proveedorId) : null;
                if (!(receipt instanceof Map<?, ?> result)) {
                    if (entrega.enviadaAt != null && entrega.enviadaAt.isBefore(LocalDateTime.now().minusHours(23))) {
                        entrega.estado = "fallida"; entrega.ultimoError = "Recibo no disponible";
                    } else entrega.proximaEjecucion = LocalDateTime.now().plusMinutes(15);
                } else if ("ok".equals(result.get("status"))) {
                    entrega.estado = "confirmada"; entrega.ultimoError = null;
                } else {
                    Object details = result.get("details");
                    Object error = details instanceof Map<?, ?> info ? info.get("error") : null;
                    entrega.ultimoError = error instanceof String value ? value : "Proveedor rechazó entrega";
                    if ("DeviceNotRegistered".equals(error)) dispositivos.findById(entrega.dispositivoId)
                            .ifPresent(d -> { d.activo = false; dispositivos.save(d); });
                    entrega.estado = "fallida";
                }
            } catch (RuntimeException ex) {
                entrega.intentos++;
                entrega.ultimoError = ex.getClass().getSimpleName();
                if (entrega.intentos >= 5) entrega.estado = "fallida";
                else entrega.proximaEjecucion = LocalDateTime.now().plusMinutes(1L << entrega.intentos);
                log.warn("Consulta de recibo push falló: entrega={}, intento={}", entrega.id, entrega.intentos);
            }
            entregas.save(entrega);
        }
    }
}
