package com.lavarapido.backend_vehicular.pagos.service;

import tools.jackson.databind.JsonNode;
import com.lavarapido.backend_vehicular.pagos.config.WompiProperties;
import com.lavarapido.backend_vehicular.pagos.dto.PagoResponseDTO;
import com.lavarapido.backend_vehicular.pagos.dto.PagoWidgetResponseDTO;
import com.lavarapido.backend_vehicular.pagos.entity.Pago;
import com.lavarapido.backend_vehicular.pagos.enums.EstadoPago;
import com.lavarapido.backend_vehicular.pagos.repository.PagoRepository;
import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.reservas.repository.ReservaRepository;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.users.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoService {
    private static final String COP = "COP";
    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final WompiProperties wompiProperties;
    private final WompiSignatureService signatureService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Transactional
    public PagoWidgetResponseDTO iniciar(UUID idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        validarPropietarioOAdmin(reserva, obtenerUsuarioAutenticado());
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new IllegalStateException("Solo se puede pagar una reserva en estado PENDIENTE");
        }
        if (pagoRepository.existsByReserva_IdReserva(idReserva)) {
            throw new IllegalStateException("La reserva ya tiene un pago asociado");
        }
        BigDecimal monto = reserva.getServicio().getPrecio();
        if (monto == null || monto.signum() <= 0 || monto.stripTrailingZeros().scale() > 0) {
            throw new IllegalStateException("El precio del servicio debe ser un valor positivo entero en COP");
        }
        long montoEnCentavos;
        try { montoEnCentavos = monto.movePointRight(2).longValueExact(); }
        catch (ArithmeticException e) { throw new IllegalStateException("El monto del servicio no es válido", e); }

        String referencia = "PAGO-" + UUID.randomUUID();
        Pago pago = pagoRepository.save(Pago.builder().reserva(reserva).monto(monto)
                .referenciaPago(referencia).metodoPago("online").estado(EstadoPago.pendiente).build());
        return new PagoWidgetResponseDTO(pago.getIdPago(), idReserva, referencia, montoEnCentavos, COP,
                wompiProperties.getPublicKey(), signatureService.crearFirmaIntegridad(referencia, montoEnCentavos, COP),
                frontendUrl + "/pagos/resultado", "NEQUI");
    }

    @Transactional(readOnly = true)
    public PagoResponseDTO obtenerPorReserva(UUID idReserva) {
        Pago pago = pagoRepository.findByReserva_IdReserva(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado para la reserva"));
        validarPropietarioOAdmin(pago.getReserva(), obtenerUsuarioAutenticado());
        return response(pago);
    }

    @Transactional
    public void procesarEvento(JsonNode evento) {
        if (!"transaction.updated".equals(evento.path("event").asString())) {
            log.info("Evento Wompi válido ignorado: tipo no procesado");
            return;
        }
        JsonNode transaction = evento.path("data").path("transaction");
        String referencia = transaction.path("reference").asString();
        if (referencia.isBlank()) {
            log.warn("Evento transaction.updated válido ignorado: sin referencia");
            return;
        }
        Pago pago = pagoRepository.findByReferenciaPago(referencia).orElse(null);
        if (pago == null) {
            log.info("Evento Wompi válido ignorado: referencia no registrada={}", referencia);
            return;
        }

        long montoRecibido = transaction.has("amount_in_cents")
                ? transaction.path("amount_in_cents").asLong(-1) : transaction.path("amountInCents").asLong(-1);
        if (montoRecibido != pago.getMonto().movePointRight(2).longValue()) {
            throw new IllegalStateException("El monto recibido de Wompi no coincide con el pago registrado");
        }
        String metodo = transaction.path("payment_method_type").asString(transaction.path("paymentMethodType").asString());
        String estadoWompi = transaction.path("status").asString();
        // Evento auténtico, pero de un método que este módulo no procesa.
        // Se ignora para responder 2xx y evitar reintentos de Wompi.
        if (!"NEQUI".equals(metodo)) {
            log.warn("Evento Wompi válido ignorado: referencia={}, método={}", referencia, metodo);
            return;
        }
        pago.setEstadoWompi(estadoWompi);
        if ("APPROVED".equals(estadoWompi)) {
            pago.setEstado(EstadoPago.aprobado);
            if (pago.getFechaPago() == null) pago.setFechaPago(LocalDateTime.now());
            Reserva reserva = pago.getReserva();
            if (reserva.getEstado() == EstadoReserva.PENDIENTE) reserva.setEstado(EstadoReserva.ASIGNADA);
        } else if ("DECLINED".equals(estadoWompi) || "VOIDED".equals(estadoWompi) || "ERROR".equals(estadoWompi)) {
            pago.setEstado(EstadoPago.rechazado);
        }
        pagoRepository.save(pago);
        log.info("Pago Wompi actualizado: referencia={}, estadoWompi={}, estadoPago={}, estadoReserva={}",
                referencia, estadoWompi, pago.getEstado(), pago.getReserva().getEstado());
    }

    private PagoResponseDTO response(Pago pago) {
        return new PagoResponseDTO(pago.getIdPago(), pago.getReserva().getIdReserva(), pago.getReferenciaPago(),
                pago.getMetodoPago(), pago.getMonto(), pago.getEstadoWompi(), pago.getEstado().name(), pago.getFechaPago());
    }

    private User obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new IllegalArgumentException("Usuario no autenticado");
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado"));
    }

    private void validarPropietarioOAdmin(Reserva reserva, User usuario) {
        if (!reserva.getUsuario().getUserId().equals(usuario.getUserId()) && !esAdmin(usuario)) {
            throw new AccessDeniedException("No tienes permiso para acceder al pago de esta reserva");
        }
    }

    private boolean esAdmin(User usuario) {
        return userRoleRepository.findActiveRoleByUserId(usuario.getUserId())
                .map(ur -> "ADMIN".equals(ur.getRole().getRoleName())).orElse(false);
    }
}
