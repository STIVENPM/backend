package com.lavarapido.backend_vehicular.asignaciones.service;

import com.lavarapido.backend_vehicular.asignaciones.dto.AsignacionEstadoRequestDTO;
import com.lavarapido.backend_vehicular.asignaciones.dto.AsignacionRequestDTO;
import com.lavarapido.backend_vehicular.asignaciones.dto.AsignacionResponseDTO;
import com.lavarapido.backend_vehicular.asignaciones.entity.Asignacion;
import com.lavarapido.backend_vehicular.asignaciones.enums.EstadoAsignacion;
import com.lavarapido.backend_vehicular.asignaciones.repository.AsignacionRepository;
import com.lavarapido.backend_vehicular.operadores.entity.Operador;
import com.lavarapido.backend_vehicular.operadores.repository.OperadorRepository;
import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.reservas.repository.ReservaRepository;
import com.lavarapido.backend_vehicular.reservas.service.ReservaService;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsignacionService {

    private final AsignacionRepository asignacionRepository;
    private final ReservaRepository reservaRepository;
    private final OperadorRepository operadorRepository;
    private final UserRepository userRepository;
    private final ReservaService reservaService;

    @Transactional
    public AsignacionResponseDTO crear(AsignacionRequestDTO request) {
        Reserva reserva = reservaRepository.findById(request.idReserva())
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        if (reserva.getEstado() != EstadoReserva.PENDIENTE && reserva.getEstado() != EstadoReserva.ASIGNADA) {
            throw new IllegalStateException("La reserva debe estar PENDIENTE o ASIGNADA para recibir una asignación");
        }

        Operador operador = operadorRepository.findById(request.idOperador())
                .orElseThrow(() -> new RecursoNoEncontradoException("Operador no encontrado"));
        if (!Boolean.TRUE.equals(operador.getEstado())) {
            throw new IllegalStateException("El operador está inactivo y no puede recibir asignaciones");
        }
        if (asignacionRepository.existsByReserva_IdReserva(reserva.getIdReserva())) {
            throw new IllegalStateException("La reserva ya tiene una asignación asociada");
        }

        if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
            reservaService.cambiarEstado(reserva.getIdReserva(), EstadoReserva.ASIGNADA);
        }

        Asignacion asignacion = Asignacion.builder()
                .reserva(reserva)
                .operador(operador)
                .estado(EstadoAsignacion.asignada)
                .build();
        return mapearAResponse(asignacionRepository.save(asignacion));
    }

    @Transactional(readOnly = true)
    public List<AsignacionResponseDTO> obtenerMisAsignaciones() {
        Operador operador = obtenerOperadorAutenticado();
        return asignacionRepository.findByOperador_IdOperadorOrderByFechaAsignacionDesc(operador.getIdOperador())
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional
    public AsignacionResponseDTO cambiarEstado(UUID idAsignacion, AsignacionEstadoRequestDTO request) {
        Operador operador = obtenerOperadorAutenticado();
        Asignacion asignacion = asignacionRepository.findById(idAsignacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación no encontrada"));

        if (!asignacion.getOperador().getIdOperador().equals(operador.getIdOperador())) {
            throw new AccessDeniedException("No tienes permiso para modificar esta asignación");
        }

        validarTransicion(asignacion.getEstado(), request.estado());
        asignacion.setEstado(request.estado());

        if (request.estado() == EstadoAsignacion.en_proceso) {
            reservaService.cambiarEstado(asignacion.getReserva().getIdReserva(), EstadoReserva.EN_PROCESO);
        }
        if (request.estado() == EstadoAsignacion.completada) {
            reservaService.cambiarEstado(asignacion.getReserva().getIdReserva(), EstadoReserva.FINALIZADA);
        }

        return mapearAResponse(asignacionRepository.save(asignacion));
    }

    private Operador obtenerOperadorAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        User usuario = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado"));
        return operadorRepository.findByUsuario_UserId(usuario.getUserId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario autenticado no tiene un operador asociado"));
    }

    private void validarTransicion(EstadoAsignacion estadoActual, EstadoAsignacion nuevoEstado) {
        boolean permitida = (estadoActual == EstadoAsignacion.asignada
                && (nuevoEstado == EstadoAsignacion.en_proceso || nuevoEstado == EstadoAsignacion.cancelada))
                || (estadoActual == EstadoAsignacion.en_proceso
                && (nuevoEstado == EstadoAsignacion.completada || nuevoEstado == EstadoAsignacion.cancelada));
        if (!permitida) {
            throw new IllegalStateException("Transición de estado de asignación no permitida: "
                    + estadoActual + " -> " + nuevoEstado);
        }
    }

    private AsignacionResponseDTO mapearAResponse(Asignacion asignacion) {
        User usuario = asignacion.getOperador().getUsuario();
        String nombreOperador = usuario.getFirstName();
        if (usuario.getLastName() != null && !usuario.getLastName().isBlank()) {
            nombreOperador += " " + usuario.getLastName();
        }
        return new AsignacionResponseDTO(asignacion.getIdAsignacion(), asignacion.getReserva().getIdReserva(),
                asignacion.getOperador().getIdOperador(), nombreOperador, asignacion.getEstado(),
                asignacion.getFechaAsignacion(), asignacion.getCreatedAt(), asignacion.getUpdatedAt());
    }
}
