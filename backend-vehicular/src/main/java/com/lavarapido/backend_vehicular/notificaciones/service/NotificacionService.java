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
import org.springframework.security.access.AccessDeniedException;
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
        String bloqueoToken = "push-token:" + token;
        String bloqueoInstalacion = "push-installation:" + usuario.getUserId() + ":" + instalacion;
        java.util.stream.Stream.of(bloqueoToken, bloqueoInstalacion).sorted()
                .forEach(dispositivos::bloquearRegistro);

        DispositivoPush porToken = dispositivos.findByToken(token).orElse(null);
        DispositivoPush porInstalacion = dispositivos
                .findByUsuarioIdAndInstalacionId(usuario.getUserId(), instalacion).orElse(null);

        if (porToken != null && !porToken.usuarioId.equals(usuario.getUserId())) {
            // Un cambio de cuenta solo puede recuperar el mismo dispositivo después
            // de que la cuenta anterior lo haya desactivado explícitamente.
            if (porToken.activo || !porToken.instalacionId.equals(instalacion)) {
                throw new AccessDeniedException("El token pertenece a otro usuario");
            }
        }

        DispositivoPush destino = porToken != null ? porToken : porInstalacion;
        if (porToken != null && porInstalacion != null && porToken != porInstalacion
                && !porToken.id.equals(porInstalacion.id)) {
            // Conserva la fila histórica referenciada por entregas, pero libera la
            // combinación usuario/instalación antes de reutilizar la fila del token.
            porInstalacion.activo = false;
            porInstalacion.token = "revoked-" + porInstalacion.id;
            porInstalacion.instalacionId = "revoked-" + porInstalacion.id;
            dispositivos.saveAndFlush(porInstalacion);
        }
        if (destino == null) destino = new DispositivoPush();
        destino.usuarioId = usuario.getUserId();
        destino.instalacionId = instalacion;
        destino.token = token;
        destino.activo = true;
        dispositivos.save(destino);
    }

    @Transactional
    public void desactivarDispositivo(String instalacion) {
        dispositivos.findByUsuarioIdAndInstalacionId(actual().getUserId(), instalacion)
                .ifPresent(d -> { d.activo = false; dispositivos.save(d); });
    }
}
