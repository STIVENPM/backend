
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

import java.math.BigDecimal;
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

    // =========================================================
    // CREAR ASIGNACIÓN
    // =========================================================

    @Transactional
    public AsignacionResponseDTO crear(AsignacionRequestDTO request) {

        Reserva reserva = reservaRepository.findById(request.idReserva())
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Reserva no encontrada"));

        // La reserva debe estar pendiente o asignada
        if (reserva.getEstado() != EstadoReserva.PENDIENTE
                && reserva.getEstado() != EstadoReserva.ASIGNADA) {

            throw new IllegalStateException(
                    "La reserva debe estar PENDIENTE o ASIGNADA para recibir una asignación"
            );
        }

        // Buscar operador
        Operador operador = operadorRepository.findById(request.idOperador())
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Operador no encontrado"));

        // Verificar que el operador esté activo
        if (!Boolean.TRUE.equals(operador.getEstado())) {

            throw new IllegalStateException(
                    "El operador está inactivo y no puede recibir asignaciones"
            );
        }

        // Verificar que la reserva no tenga otra asignación
        if (asignacionRepository.existsByReserva_IdReserva(
                reserva.getIdReserva())) {

            throw new IllegalStateException(
                    "La reserva ya tiene una asignación asociada"
            );
        }

        // Si la reserva está pendiente, cambiarla a asignada
        if (reserva.getEstado() == EstadoReserva.PENDIENTE) {

            reservaService.cambiarEstado(
                    reserva.getIdReserva(),
                    EstadoReserva.ASIGNADA
            );
        }

        // Crear asignación
        Asignacion asignacion = Asignacion.builder()
                .reserva(reserva)
                .operador(operador)
                .estado(EstadoAsignacion.asignada)
                .build();

        return mapearAResponse(
                asignacionRepository.save(asignacion)
        );
    }

    // =========================================================
    // OBTENER MIS ASIGNACIONES
    // =========================================================

    @Transactional(readOnly = true)
    public List<AsignacionResponseDTO> obtenerMisAsignaciones() {

        // Obtener el operador correspondiente al usuario autenticado
        Operador operador = obtenerOperadorAutenticado();

        return asignacionRepository
                .findByOperador_IdOperadorOrderByFechaAsignacionDesc(
                        operador.getIdOperador()
                )
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    // =========================================================
    // CAMBIAR ESTADO DE ASIGNACIÓN
    // =========================================================

    @Transactional
    public AsignacionResponseDTO cambiarEstado(
            UUID idAsignacion,
            AsignacionEstadoRequestDTO request) {

        // Obtener operador autenticado
        Operador operador = obtenerOperadorAutenticado();

        // Buscar asignación
        Asignacion asignacion = asignacionRepository.findById(idAsignacion)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Asignación no encontrada"
                        ));

        // Verificar que la asignación pertenezca al operador
        if (!asignacion.getOperador()
                .getIdOperador()
                .equals(operador.getIdOperador())) {

            throw new AccessDeniedException(
                    "No tienes permiso para modificar esta asignación"
            );
        }

        // Validar transición
        validarTransicion(
                asignacion.getEstado(),
                request.estado()
        );

        // Actualizar estado
        asignacion.setEstado(request.estado());

        // Si comienza el servicio
        if (request.estado() == EstadoAsignacion.en_proceso) {

            reservaService.cambiarEstado(
                    asignacion.getReserva().getIdReserva(),
                    EstadoReserva.EN_PROCESO
            );
        }

        // Si finaliza el servicio
        if (request.estado() == EstadoAsignacion.completada) {

            reservaService.cambiarEstado(
                    asignacion.getReserva().getIdReserva(),
                    EstadoReserva.FINALIZADA
            );
        }

        return mapearAResponse(
                asignacionRepository.save(asignacion)
        );
    }

    // =========================================================
    // OBTENER OPERADOR AUTENTICADO
    // =========================================================

    private Operador obtenerOperadorAutenticado() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || authentication.getName() == null) {

            throw new IllegalArgumentException(
                    "Usuario no autenticado"
            );
        }

        // El nombre del Authentication corresponde al email
        User usuario = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Usuario autenticado no encontrado"
                        ));

        // Buscar el operador relacionado con el usuario
        return operadorRepository
                .findByUsuario_UserId(usuario.getUserId())
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El usuario autenticado no tiene un operador asociado"
                        ));
    }

    // =========================================================
    // VALIDAR TRANSICIONES DE ESTADO
    // =========================================================

    private void validarTransicion(
            EstadoAsignacion estadoActual,
            EstadoAsignacion nuevoEstado) {

        boolean permitida =

                // ASIGNADA -> EN_PROCESO
                // ASIGNADA -> CANCELADA
                (
                        estadoActual == EstadoAsignacion.asignada
                                && (
                                nuevoEstado == EstadoAsignacion.en_proceso
                                        || nuevoEstado == EstadoAsignacion.cancelada
                        )
                )

                ||

                // EN_PROCESO -> COMPLETADA
                // EN_PROCESO -> CANCELADA
                (
                        estadoActual == EstadoAsignacion.en_proceso
                                && (
                                nuevoEstado == EstadoAsignacion.completada
                                        || nuevoEstado == EstadoAsignacion.cancelada
                        )
                );

        if (!permitida) {

            throw new IllegalStateException(
                    "Transición de estado de asignación no permitida: "
                            + estadoActual
                            + " -> "
                            + nuevoEstado
            );
        }
    }

    // =========================================================
    // MAPEAR ASIGNACIÓN A RESPONSE DTO
    // =========================================================

    private AsignacionResponseDTO mapearAResponse(
            Asignacion asignacion) {

        // =====================================================
        // DATOS PRINCIPALES
        // =====================================================

        Reserva reserva = asignacion.getReserva();

        User usuario = reserva.getUsuario();

        // =====================================================
        // NOMBRE DEL OPERADOR
        // =====================================================

        User usuarioOperador =
                asignacion.getOperador().getUsuario();

        String nombreOperador =
                usuarioOperador.getFirstName();

        if (usuarioOperador.getLastName() != null
                && !usuarioOperador.getLastName().isBlank()) {

            nombreOperador += " "
                    + usuarioOperador.getLastName();
        }

        // =====================================================
        // NOMBRE DEL CLIENTE
        // =====================================================

        String nombreCliente =
                usuario.getFirstName();

        if (usuario.getLastName() != null
                && !usuario.getLastName().isBlank()) {

            nombreCliente += " "
                    + usuario.getLastName();
        }

        // =====================================================
        // VEHÍCULO
        // =====================================================

        String placa =
                reserva.getVehiculo().getPlaca();

        String color =
                reserva.getVehiculo().getColor();

        String tipoVehiculo =
                reserva.getVehiculo()
                        .getTipoVehiculo()
                        .name();

        // =====================================================
        // SERVICIO
        // =====================================================

        UUID idServicio =
                reserva.getServicio().getIdServicio();

        String nombreServicio =
                reserva.getServicio().getNombre();

        String descripcionServicio =
                reserva.getServicio().getDescripcion();

        BigDecimal precioServicio =
                reserva.getServicio().getPrecio();

        Integer duracionMinutos =
                reserva.getServicio().getDuracionMinutos();

        // =====================================================
        // RESPONSE
        // =====================================================

        return new AsignacionResponseDTO(

                // -------------------------------------------------
                // ASIGNACIÓN
                // -------------------------------------------------

                asignacion.getIdAsignacion(),

                reserva.getIdReserva(),

                asignacion.getOperador().getIdOperador(),

                nombreOperador,

                asignacion.getEstado(),

                asignacion.getFechaAsignacion(),

                // -------------------------------------------------
                // CLIENTE
                // -------------------------------------------------

                nombreCliente,

                // -------------------------------------------------
                // VEHÍCULO
                // -------------------------------------------------

                placa,

                color,

                tipoVehiculo,

                // -------------------------------------------------
                // SERVICIO
                // -------------------------------------------------

                idServicio,

                nombreServicio,

                descripcionServicio,

                precioServicio,

                duracionMinutos,

                // -------------------------------------------------
                // RESERVA
                // -------------------------------------------------

                reserva.getFechaReserva(),

                reserva.getHoraReserva(),

                // -------------------------------------------------
                // AUDITORÍA
                // -------------------------------------------------

                asignacion.getCreatedAt(),

                asignacion.getUpdatedAt()
        );
    }
}
