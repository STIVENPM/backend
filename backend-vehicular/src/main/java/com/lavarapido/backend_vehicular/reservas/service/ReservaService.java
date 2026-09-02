package com.lavarapido.backend_vehicular.reservas.service;

import com.lavarapido.backend_vehicular.reservas.dto.ReservaRequestDTO;
import com.lavarapido.backend_vehicular.reservas.dto.ReservaResponseDTO;
import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.reservas.exception.EstadoReservaInvalidoException;
import com.lavarapido.backend_vehicular.reservas.exception.HorarioReservaInvalidoException;
import com.lavarapido.backend_vehicular.reservas.exception.VehiculoNoPerteneceUsuarioException;
import com.lavarapido.backend_vehicular.reservas.repository.ReservaRepository;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
import com.lavarapido.backend_vehicular.servicios.repository.ServicioRepository;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.users.repository.UserRoleRepository;
import com.lavarapido.backend_vehicular.vehiculos.entity.Vehiculo;
import com.lavarapido.backend_vehicular.vehiculos.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ServicioRepository servicioRepository;

    @Transactional
    public ReservaResponseDTO crear(ReservaRequestDTO request) {
        User usuario = obtenerUsuarioAutenticado();

        Vehiculo vehiculo = vehiculoRepository.findById(request.getFkIdVehiculo())
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo no encontrado"));

        Servicio servicio = servicioRepository.findById(request.getFkIdServicio())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));

        if (!esAdmin(usuario) && !vehiculo.getUsuario().getUserId().equals(usuario.getUserId())) {
            throw new VehiculoNoPerteneceUsuarioException("El vehículo no pertenece al usuario autenticado");
        }

        validarHoraReserva(request.getHoraReserva());

        LocalTime horaFin = request.getHoraReserva().plusMinutes(servicio.getDuracionMinutos());
        validarHoraFinReserva(horaFin);

        List<Reserva> solapamientos = reservaRepository.findSolapamientosPorVehiculo(
                vehiculo.getIdVehiculo(),
                request.getFechaReserva(),
                request.getHoraReserva(),
                horaFin,
                EstadoReserva.CANCELADA.name()
        );

        if (!solapamientos.isEmpty()) {
            throw new HorarioReservaInvalidoException("Ya existe una reserva solapada para ese vehículo en ese horario");
        }

        Reserva reserva = Reserva.builder()
                .usuario(vehiculo.getUsuario())
                .vehiculo(vehiculo)
                .servicio(servicio)
                .fechaReserva(request.getFechaReserva())
                .horaReserva(request.getHoraReserva())
                .estado(EstadoReserva.PENDIENTE)
                .build();

        return mapearAResponse(reservaRepository.save(reserva));
    }

    public ReservaResponseDTO obtenerPorId(UUID idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        validarPropietarioOAdmin(reserva, obtenerUsuarioAutenticado());
        return mapearAResponse(reserva);
    }

    public List<ReservaResponseDTO> obtenerPorUsuario(UUID idUsuario) {
        User usuarioSolicitado = userRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        User usuarioAutenticado = obtenerUsuarioAutenticado();

        if (!usuarioSolicitado.getUserId().equals(usuarioAutenticado.getUserId()) && !esAdmin(usuarioAutenticado)) {
            throw new AccessDeniedException("No tienes permiso para consultar las reservas de este usuario");
        }

        return reservaRepository.findByUsuario_UserIdOrderByFechaReservaDescHoraReservaDesc(idUsuario)
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    public List<ReservaResponseDTO> obtenerTodas() {
        return reservaRepository.findAll()
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional
    public ReservaResponseDTO cambiarEstado(UUID idReserva, EstadoReserva nuevoEstado) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));

        validarTransicion(reserva.getEstado(), nuevoEstado);
        reserva.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoReserva.EN_PROCESO && reserva.getFechaHoraInicio() == null) {
            reserva.setFechaHoraInicio(LocalDateTime.now());
        }

        if (nuevoEstado == EstadoReserva.FINALIZADA && reserva.getFechaHoraFin() == null) {
            reserva.setFechaHoraFin(LocalDateTime.now());
        }

        return mapearAResponse(reservaRepository.save(reserva));
    }

    @Transactional
    public ReservaResponseDTO cancelar(UUID idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        validarPropietarioOAdmin(reserva, obtenerUsuarioAutenticado());
        return cambiarEstado(idReserva, EstadoReserva.CANCELADA);
    }

    private void validarHoraReserva(LocalTime horaReserva) {
        if (horaReserva == null) {
            throw new HorarioReservaInvalidoException("La hora de la reserva es obligatoria");
        }
        if (horaReserva.isBefore(LocalTime.of(6, 0)) || horaReserva.isAfter(LocalTime.of(20, 0))) {
            throw new HorarioReservaInvalidoException("La hora de la reserva debe estar entre 06:00 y 20:00");
        }
    }

    private void validarHoraFinReserva(LocalTime horaFin) {
        if (horaFin.isAfter(LocalTime.of(20, 0))) {
            throw new HorarioReservaInvalidoException("La reserva no puede extenderse más allá de las 20:00");
        }
    }

    private void validarTransicion(EstadoReserva estadoActual, EstadoReserva nuevoEstado) {
        if (estadoActual == null || nuevoEstado == null) {
            throw new EstadoReservaInvalidoException("El estado de la reserva es obligatorio");
        }

        if (Objects.equals(estadoActual, nuevoEstado)) {
            return;
        }

        switch (estadoActual) {
            case PENDIENTE -> {
                if (nuevoEstado == EstadoReserva.ASIGNADA || nuevoEstado == EstadoReserva.CANCELADA) {
                    return;
                }
            }
            case ASIGNADA -> {
                if (nuevoEstado == EstadoReserva.EN_PROCESO || nuevoEstado == EstadoReserva.CANCELADA) {
                    return;
                }
            }
            case EN_PROCESO -> {
                if (nuevoEstado == EstadoReserva.FINALIZADA) {
                    return;
                }
            }
            case FINALIZADA, CANCELADA -> {
                throw new EstadoReservaInvalidoException("La reserva ya se encuentra en un estado final y no puede cambiarse");
            }
            default -> throw new EstadoReservaInvalidoException("Transición de estado no permitida");
        }

        throw new EstadoReservaInvalidoException("Transición de estado no permitida: " + estadoActual + " -> " + nuevoEstado);
    }

    private User obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado"));
    }

    private boolean esAdmin(User usuario) {
        return userRoleRepository.findActiveRoleByUserId(usuario.getUserId())
                .map(userRole -> "ADMIN".equals(userRole.getRole().getRoleName()))
                .orElse(false);
    }

    private void validarPropietarioOAdmin(Reserva reserva, User usuarioAutenticado) {
        boolean esPropietario = reserva.getUsuario().getUserId().equals(usuarioAutenticado.getUserId());
        if (!esPropietario && !esAdmin(usuarioAutenticado)) {
            throw new AccessDeniedException("No tienes permiso para acceder a esta reserva");
        }
    }

    private ReservaResponseDTO mapearAResponse(Reserva reserva) {
        User usuario = reserva.getUsuario();
        Vehiculo vehiculo = reserva.getVehiculo();
        Servicio servicio = reserva.getServicio();

        String nombreUsuario = usuario.getFirstName();
        if (usuario.getLastName() != null && !usuario.getLastName().isBlank()) {
            nombreUsuario += " " + usuario.getLastName();
        }

        return new ReservaResponseDTO(
                reserva.getIdReserva(),
                usuario.getUserId(),
                nombreUsuario,
                vehiculo.getIdVehiculo(),
                vehiculo.getPlaca(),
                vehiculo.getTipoVehiculo() != null ? vehiculo.getTipoVehiculo().name() : null,
                servicio.getIdServicio(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getPrecio(),
                servicio.getDuracionMinutos(),
                reserva.getFechaReserva(),
                reserva.getHoraReserva(),
                reserva.getFechaHoraInicio(),
                reserva.getFechaHoraFin(),
                reserva.getEstado(),
                reserva.getCreatedAt(),
                reserva.getUpdatedAt()
        );
    }
}
