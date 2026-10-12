package com.lavarapido.backend_vehicular.notificaciones.service;

import com.lavarapido.backend_vehicular.notificaciones.dto.NotificacionDTO;
import com.lavarapido.backend_vehicular.notificaciones.entity.*;
import com.lavarapido.backend_vehicular.notificaciones.repository.*;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionService {
    private final NotificacionRepository notificaciones;
    private final NotificacionEntregaRepository entregas;
    private final DispositivoPushRepository dispositivos;
    private final UserRepository usuarios;
    private final AccountAccessService access;

    public NotificacionService(NotificacionRepository notificaciones, NotificacionEntregaRepository entregas,
            DispositivoPushRepository dispositivos, UserRepository usuarios, AccountAccessService access) {
        this.notificaciones = notificaciones;
        this.entregas = entregas;
        this.dispositivos = dispositivos;
        this.usuarios = usuarios;
        this.access = access;
    }

    private User actual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new org.springframework.security.access.AccessDeniedException("Sin sesión");
        return access.activeUser(auth.getName());
    }

    @Transactional(readOnly = true)
    public Page<NotificacionDTO> listar(int pagina, int tamano, boolean soloNoLeidas) {
        if (pagina < 0 || tamano < 1 || tamano > 50) throw new IllegalArgumentException("Paginación inválida");
        UUID id = actual().getUserId();
        var pageable = PageRequest.of(pagina, tamano);
        return (soloNoLeidas
                ? notificaciones.findByUsuarioIdAndLeidaAtIsNullOrderByCreatedAtDesc(id, pageable)
                : notificaciones.findByUsuarioIdOrderByCreatedAtDesc(id, pageable)).map(NotificacionDTO::from);
    }

    @Transactional(readOnly = true)
    public long noLeidas() { return notificaciones.countByUsuarioIdAndLeidaAtIsNull(actual().getUserId()); }

    @Transactional
    public NotificacionDTO marcarLeida(UUID id) {
        Notificacion n = notificaciones.findByIdAndUsuarioId(id, actual().getUserId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación no encontrada"));
        if (n.leidaAt == null) n.leidaAt = LocalDateTime.now();
        return NotificacionDTO.from(notificaciones.save(n));
    }

    @Transactional
    public int marcarTodas() { return notificaciones.marcarTodas(actual().getUserId()); }

    // Se invoca dentro de la transacción del cambio de negocio.
    @Transactional
    public void crear(User destinatario, String clave, String tipo, String titulo, String mensaje,
                      UUID reservaId, UUID pagoId) {
        if (!Boolean.TRUE.equals(destinatario.getStatus())
                || notificaciones.existsByUsuarioIdAndClaveEvento(destinatario.getUserId(), clave)) return;
        Notificacion n = new Notificacion();
        n.usuarioId = destinatario.getUserId(); n.claveEvento = clave; n.tipo = tipo;
        n.titulo = titulo; n.mensaje = mensaje; n.reservaId = reservaId; n.pagoId = pagoId;
        n = notificaciones.save(n);
        NotificacionEntrega email = new NotificacionEntrega();
        email.notificacionId = n.id; email.canal = "email";
        entregas.save(email);
        for (DispositivoPush d : dispositivos.findByUsuarioIdAndActivoTrue(destinatario.getUserId())) {
            NotificacionEntrega push = new NotificacionEntrega();
            push.notificacionId = n.id; push.canal = "push"; push.dispositivoId = d.id;
            entregas.save(push);
        }
    }

    @Transactional
    public void alertarAdministradores(String clave, String titulo, String mensaje, UUID reservaId, UUID pagoId) {
        for (User u : usuarios.findActiveAdministratorCandidates()) {
            try {
                if ("ADMIN".equals(access.currentRole(u)))
                    crear(u, clave, "system", titulo, mensaje, reservaId, pagoId);
            } catch (org.springframework.security.authentication.BadCredentialsException ignored) {
                // Una cuenta con roles revocados o inconsistentes no recibe avisos administrativos.
            }
        }
    }

    @Transactional
    public void registrarDispositivo(String instalacion, String token) {
        if (instalacion == null || !instalacion.matches("[a-zA-Z0-9_-]{8,100}")
                || token == null || token.length() > 255
                || !(token.matches("Expo(nent)?PushToken\\[[A-Za-z0-9_-]+\\]"))) {
            throw new IllegalArgumentException("Dispositivo inválido");
        }
        User usuario = actual();
        dispositivos.findByToken(token).ifPresent(anterior -> {
            if (!anterior.usuarioId.equals(usuario.getUserId())
                    || !anterior.instalacionId.equals(instalacion)) {
                anterior.activo = false;
                anterior.token = "revoked-" + anterior.id;
                dispositivos.saveAndFlush(anterior);
            }
        });
        DispositivoPush d = dispositivos.findByUsuarioIdAndInstalacionId(usuario.getUserId(), instalacion)
                .orElseGet(DispositivoPush::new);
        d.usuarioId = usuario.getUserId(); d.instalacionId = instalacion;
        d.token = token; d.activo = true;
        dispositivos.save(d);
    }

    @Transactional
    public void desactivarDispositivo(String instalacion) {
        dispositivos.findByUsuarioIdAndInstalacionId(actual().getUserId(), instalacion)
                .ifPresent(d -> { d.activo = false; dispositivos.save(d); });
    }
}
