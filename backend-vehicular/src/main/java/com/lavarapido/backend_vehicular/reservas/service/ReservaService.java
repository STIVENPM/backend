package com.lavarapido.backend_vehicular.reservas.service;

import com.lavarapido.backend_vehicular.asignaciones.entity.Asignacion;
import com.lavarapido.backend_vehicular.asignaciones.repository.AsignacionRepository;
import com.lavarapido.backend_vehicular.reservas.dto.ReservaRequestDTO;
import com.lavarapido.backend_vehicular.reservas.dto.ReservaResponseDTO;
import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.reservas.exception.EstadoReservaInvalidoException;
import com.lavarapido.backend_vehicular.reservas.exception.HorarioReservaInvalidoException;
import com.lavarapido.backend_vehicular.reservas.exception.ReservaCancelacionConflictException;
import com.lavarapido.backend_vehicular.reservas.exception.VehiculoNoPerteneceUsuarioException;
import com.lavarapido.backend_vehicular.reservas.repository.ReservaRepository;
import com.lavarapido.backend_vehicular.pagos.entity.Pago;
import com.lavarapido.backend_vehicular.pagos.entity.PagoIntento;
import com.lavarapido.backend_vehicular.pagos.enums.EstadoIntentoPago;
import com.lavarapido.backend_vehicular.pagos.enums.EstadoPago;
import com.lavarapido.backend_vehicular.pagos.repository.PagoIntentoRepository;
import com.lavarapido.backend_vehicular.pagos.repository.PagoRepository;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
import com.lavarapido.backend_vehicular.servicios.repository.ServicioRepository;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.notificaciones.service.NotificacionService;
import com.lavarapido.backend_vehicular.vehiculos.entity.Vehiculo;
import com.lavarapido.backend_vehicular.vehiculos.repository.VehiculoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final UserRepository userRepository;
    private final AccountAccessService accountAccessService;
    private final VehiculoRepository vehiculoRepository;
    private final ServicioRepository servicioRepository;
    private final AsignacionRepository asignacionRepository;
    private final NotificacionService notificacionService;
    private final PagoRepository pagoRepository;
    private final PagoIntentoRepository pagoIntentoRepository;
    private static final Set<String> TERMINAL_WOMPI_FAILURES = Set.of("DECLINED", "VOIDED", "ERROR");

    public ReservaService(ReservaRepository reservaRepository, UserRepository userRepository, AccountAccessService accountAccessService, VehiculoRepository vehiculoRepository, ServicioRepository servicioRepository, AsignacionRepository asignacionRepository, NotificacionService notificacionService, PagoRepository pagoRepository, PagoIntentoRepository pagoIntentoRepository) { this.reservaRepository=reservaRepository; this.userRepository=userRepository; this.accountAccessService=accountAccessService; this.vehiculoRepository=vehiculoRepository; this.servicioRepository=servicioRepository; this.asignacionRepository=asignacionRepository; this.notificacionService=notificacionService; this.pagoRepository=pagoRepository; this.pagoIntentoRepository=pagoIntentoRepository; }

    // Comprueba dueño, disponibilidad y horario; guarda precio y duración pactados.
    @Transactional
    public ReservaResponseDTO crear(ReservaRequestDTO request) {
        User usuario = obtenerUsuarioAutenticado();

        Vehiculo vehiculo = vehiculoRepository.findByIdForUpdate(request.getFkIdVehiculo())
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo no encontrado"));

        Servicio servicio = servicioRepository.findByIdForUpdate(request.getFkIdServicio())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));

        if (!esAdmin(usuario) && !vehiculo.getUsuario().getUserId().equals(usuario.getUserId())) {
            throw new VehiculoNoPerteneceUsuarioException("El vehículo no pertenece al usuario autenticado");
        }

        if (!Boolean.TRUE.equals(vehiculo.getEstado())) {
            throw new IllegalStateException("El vehículo está inactivo y no puede reservarse");
        }

        if (!Boolean.TRUE.equals(servicio.getEstado())) {
            throw new IllegalStateException("El servicio está inactivo y no puede reservarse");
        }

        validarHoraReserva(request.getHoraReserva());

        LocalTime horaFin = request.getHoraReserva().plusMinutes(servicio.getDuracionMinutos());
        validarHoraFinReserva(horaFin);

        // Busca otra reserva del mismo vehículo que ocupe este intervalo.
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

        // Copia precio y duración actuales para que cambios del catálogo no alteren esta reserva.
        Reserva reserva = Reserva.builder()
                .usuario(vehiculo.getUsuario())
                .vehiculo(vehiculo)
                .servicio(servicio)
                .fechaReserva(request.getFechaReserva())
                .horaReserva(request.getHoraReserva())
                .precioPactado(servicio.getPrecio())
                .duracionMinutosPactada(servicio.getDuracionMinutos())
                .estado(EstadoReserva.PENDIENTE)
                .build();

        reserva = reservaRepository.save(reserva);
        notificacionService.crear(reserva.getUsuario(), "reserva:" + reserva.getIdReserva() + ":creada",
                "booking", "Reserva creada", "Tu reserva está pendiente.", reserva.getIdReserva(), null);
        return mapearAResponse(reserva);
    }

    // Solo el dueño o un admin puede ver esta reserva.
    public ReservaResponseDTO obtenerPorId(UUID idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        validarPropietarioOAdmin(reserva, obtenerUsuarioAutenticado());

        String operadorNombre = asignacionRepository.findByReserva_IdReserva(idReserva)
                .map(this::mapearNombreOperador)
                .orElse(null);

        return mapearAResponse(reserva, operadorNombre);
    }

    // Protege la lista de reservas de otro usuario.
    public List<ReservaResponseDTO> obtenerPorUsuario(UUID idUsuario) {
        User usuarioSolicitado = userRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        User usuarioAutenticado = obtenerUsuarioAutenticado();

        if (!usuarioSolicitado.getUserId().equals(usuarioAutenticado.getUserId()) && !esAdmin(usuarioAutenticado)) {
            throw new AccessDeniedException("No tienes permiso para consultar las reservas de este usuario");
        }

        List<Reserva> reservas = reservaRepository.findByUsuario_UserIdOrderByFechaReservaDescHoraReservaDesc(idUsuario);
        return mapearReservas(reservas);
    }

    // Lista reservas para el panel administrador.
    public List<ReservaResponseDTO> obtenerTodas() {
        List<Reserva> reservas = reservaRepository.findAll();
        return mapearReservas(reservas);
    }

    // Aplica la máquina de estados y guarda marcas de tiempo.
    @Transactional
    public ReservaResponseDTO cambiarEstado(UUID idReserva, EstadoReserva nuevoEstado) {
        Reserva reserva = reservaRepository.findByIdForUpdate(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        return cambiarEstadoBloqueado(reserva, nuevoEstado);
    }

    private ReservaResponseDTO cambiarEstadoBloqueado(Reserva reserva, EstadoReserva nuevoEstado) {
        UUID idReserva = reserva.getIdReserva();
        EstadoReserva anterior = reserva.getEstado();
        validarTransicion(reserva.getEstado(), nuevoEstado);
        if (anterior == nuevoEstado) return mapearAResponse(reserva);
        if (nuevoEstado == EstadoReserva.CANCELADA) {
            validarPagoCancelable(idReserva);
        }
        reserva.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoReserva.CANCELADA) {
            asignacionRepository.findByReserva_IdReserva(idReserva).ifPresent(asignacion -> {
                if (asignacion.getEstado() == com.lavarapido.backend_vehicular.asignaciones.enums.EstadoAsignacion.asignada) {
                    asignacion.setEstado(com.lavarapido.backend_vehicular.asignaciones.enums.EstadoAsignacion.cancelada);
                    asignacionRepository.save(asignacion);
                }
            });
        }

        if (nuevoEstado == EstadoReserva.EN_PROCESO && reserva.getFechaHoraInicio() == null) {
            reserva.setFechaHoraInicio(LocalDateTime.now());
        }

        if (nuevoEstado == EstadoReserva.FINALIZADA && reserva.getFechaHoraFin() == null) {
            reserva.setFechaHoraFin(LocalDateTime.now());
        }

        reserva = reservaRepository.save(reserva);
        String titulo = switch (nuevoEstado) {
            case ASIGNADA -> "Reserva asignada";
            case EN_PROCESO -> "Servicio iniciado";
            case FINALIZADA -> "Servicio finalizado";
            case CANCELADA -> "Reserva cancelada";
            default -> "Reserva actualizada";
        };
        notificacionService.crear(reserva.getUsuario(), "reserva:" + idReserva + ":" + nuevoEstado,
                nuevoEstado == EstadoReserva.ASIGNADA || nuevoEstado == EstadoReserva.CANCELADA ? "booking" : "status",
                titulo, titulo + ".", idReserva, null);
        return mapearAResponse(reserva);
    }

    // Reutiliza la transición de estado hacia CANCELADA.
    @Transactional
    public ReservaResponseDTO cancelar(UUID idReserva) {
        return cancelarConResultado(idReserva).reserva();
    }

    @Transactional
    public ResultadoCancelacion cancelarConResultado(UUID idReserva) {
        Reserva reserva = reservaRepository.findByIdForUpdate(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        validarPropietarioOAdmin(reserva, obtenerUsuarioAutenticado());
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            return new ResultadoCancelacion(mapearAResponse(reserva), true);
        }
        return new ResultadoCancelacion(cambiarEstadoBloqueado(reserva, EstadoReserva.CANCELADA), false);
    }

    private void validarPagoCancelable(UUID idReserva) {
        Pago pago = pagoRepository.findByReservaIdForUpdate(idReserva).orElse(null);
        if (pago == null) return;

        List<PagoIntento> intentos = pagoIntentoRepository
                .findByPago_IdPagoOrderByCreatedAtAsc(pago.getIdPago());
        boolean tieneAprobacion = pago.getEstado() == EstadoPago.aprobado
                || intentos.stream().anyMatch(intento ->
                intento.getEstado() == EstadoIntentoPago.aprobado
                        || intento.getEstado() == EstadoIntentoPago.aprobado_duplicado);
        if (tieneAprobacion) {
            throw new ReservaCancelacionConflictException(
                    "PAGO_APROBADO",
                    "La reserva tiene un pago aprobado y no puede cancelarse");
        }

        boolean estadosRemotosFinales = !intentos.isEmpty() && intentos.stream().allMatch(intento ->
                intento.getEstado() == EstadoIntentoPago.rechazado
                        && intento.getWompiStatus() != null
                        && TERMINAL_WOMPI_FAILURES.contains(intento.getWompiStatus().toUpperCase(Locale.ROOT)));
        if (pago.getEstado() != EstadoPago.rechazado || !estadosRemotosFinales) {
            throw new ReservaCancelacionConflictException(
                    "PAGO_PENDIENTE",
                    "El pago aún no tiene un estado final confirmado por Wompi");
        }
    }

    public record ResultadoCancelacion(ReservaResponseDTO reserva, boolean yaEstabaCancelada) { }

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

        return accountAccessService.activeUser(authentication.getName());
    }

    private boolean esAdmin(User usuario) {
        return "ADMIN".equals(accountAccessService.currentRole(usuario));
    }

    private void validarPropietarioOAdmin(Reserva reserva, User usuarioAutenticado) {
        boolean esPropietario = reserva.getUsuario().getUserId().equals(usuarioAutenticado.getUserId());
        if (!esPropietario && !esAdmin(usuarioAutenticado)) {
            throw new AccessDeniedException("No tienes permiso para acceder a esta reserva");
        }
    }

    private List<ReservaResponseDTO> mapearReservas(List<Reserva> reservas) {
        if (reservas == null || reservas.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> operadorNombrePorReserva = new HashMap<>();
        List<UUID> idsReservas = reservas.stream()
                .map(Reserva::getIdReserva)
                .toList();

        List<Asignacion> asignaciones = asignacionRepository.findByReserva_IdReservaIn(idsReservas);
        for (Asignacion asignacion : asignaciones) {
            operadorNombrePorReserva.put(asignacion.getReserva().getIdReserva(), mapearNombreOperador(asignacion));
        }

        return reservas.stream()
                .map(reserva -> mapearAResponse(reserva, operadorNombrePorReserva.get(reserva.getIdReserva())))
                .toList();
    }

    private String mapearNombreOperador(Asignacion asignacion) {
        User usuarioOperador = asignacion.getOperador().getUsuario();
        String nombreOperador = usuarioOperador.getFirstName();
        if (usuarioOperador.getLastName() != null && !usuarioOperador.getLastName().isBlank()) {
            nombreOperador += " " + usuarioOperador.getLastName();
        }
        return nombreOperador;
    }

    private ReservaResponseDTO mapearAResponse(Reserva reserva) {
        return mapearAResponse(reserva, null);
    }

    private ReservaResponseDTO mapearAResponse(Reserva reserva, String operadorNombre) {
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
                reserva.getPrecioPactado(),
                reserva.getDuracionMinutosPactada(),
                reserva.getFechaReserva(),
                reserva.getHoraReserva(),
                reserva.getFechaHoraInicio(),
                reserva.getFechaHoraFin(),
                reserva.getEstado(),
                reserva.getCreatedAt(),
                reserva.getUpdatedAt(),
                operadorNombre
        );
    }
}
