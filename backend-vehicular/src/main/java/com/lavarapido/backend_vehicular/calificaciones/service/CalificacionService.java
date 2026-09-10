package com.lavarapido.backend_vehicular.calificaciones.service;

import com.lavarapido.backend_vehicular.calificaciones.dto.*;
import com.lavarapido.backend_vehicular.calificaciones.entity.Calificacion;
import com.lavarapido.backend_vehicular.calificaciones.repository.CalificacionRepository;
import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.reservas.repository.ReservaRepository;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class CalificacionService {
    private final CalificacionRepository calificacionRepository; private final ReservaRepository reservaRepository; private final UserRepository userRepository;
    @Transactional public CalificacionResponseDTO crear(CalificacionRequestDTO request) {
        User usuario = usuarioActual();
        Reserva reserva = reservaRepository.findById(request.reservaId()).orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        if (!reserva.getUsuario().getUserId().equals(usuario.getUserId())) throw new AccessDeniedException("No tienes permiso para calificar esta reserva");
        if (reserva.getEstado() != EstadoReserva.FINALIZADA) throw new IllegalStateException("Solo se pueden calificar reservas finalizadas");
        if (calificacionRepository.existsByReserva_IdReserva(reserva.getIdReserva())) throw new IllegalStateException("La reserva ya tiene una calificacion");
        return map(calificacionRepository.save(Calificacion.builder().reserva(reserva).usuario(usuario).puntuacion(request.puntuacion()).comentario(request.comentario()).build()));
    }
    @Transactional(readOnly = true) public CalificacionResponseDTO obtenerPorReserva(UUID idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva).orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        User usuario = usuarioActual();
        boolean admin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!admin && !reserva.getUsuario().getUserId().equals(usuario.getUserId())) throw new AccessDeniedException("No tienes permiso para consultar esta calificacion");
        return map(calificacionRepository.findByReserva_IdReserva(idReserva).orElseThrow(() -> new RecursoNoEncontradoException("Calificacion no encontrada")));
    }
    private User usuarioActual() { String email = SecurityContextHolder.getContext().getAuthentication().getName(); return userRepository.findByEmail(email).orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado")); }
    private CalificacionResponseDTO map(Calificacion c) { return new CalificacionResponseDTO(c.getIdCalificacion(), c.getReserva().getIdReserva(), c.getUsuario().getUserId(), c.getPuntuacion(), c.getComentario(), c.getCreatedAt(), c.getUpdatedAt()); }
}
