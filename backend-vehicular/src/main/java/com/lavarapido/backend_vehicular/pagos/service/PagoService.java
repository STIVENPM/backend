package com.lavarapido.backend_vehicular.pagos.service;

import com.lavarapido.backend_vehicular.pagos.config.WompiConfigurationService;
import com.lavarapido.backend_vehicular.pagos.config.WompiProperties;
import com.lavarapido.backend_vehicular.pagos.dto.PagoIntentoResponseDTO;
import com.lavarapido.backend_vehicular.pagos.dto.PagoResponseDTO;
import com.lavarapido.backend_vehicular.pagos.dto.PagoWidgetResponseDTO;
import com.lavarapido.backend_vehicular.pagos.entity.Pago;
import com.lavarapido.backend_vehicular.pagos.entity.PagoIntento;
import com.lavarapido.backend_vehicular.pagos.enums.EstadoIntentoPago;
import com.lavarapido.backend_vehicular.pagos.enums.EstadoPago;
import com.lavarapido.backend_vehicular.pagos.exception.PagoConflictException;
import com.lavarapido.backend_vehicular.pagos.exception.WompiEventoInvalidoException;
import com.lavarapido.backend_vehicular.pagos.repository.PagoIntentoRepository;
import com.lavarapido.backend_vehicular.pagos.repository.PagoRepository;
import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.reservas.repository.ReservaRepository;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoService {
    private static final String COP = "COP";
    private static final Set<String> ESTADOS_WOMPI = Set.of("PENDING", "APPROVED", "DECLINED", "VOIDED", "ERROR");

    private final PagoRepository pagoRepository;
    private final PagoIntentoRepository intentoRepository;
    private final ReservaRepository reservaRepository;
    private final AccountAccessService accountAccessService;
    private final WompiProperties wompiProperties;
    private final WompiConfigurationService configurationService;
    private final WompiSignatureService signatureService;
    private final WompiTransactionClient transactionClient;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Bloquea la reserva y crea o reutiliza el intento pendiente de pago.
    @Transactional
    public PagoWidgetResponseDTO iniciar(UUID idReserva) {
        configurationService.validarWidget();
        Reserva reserva = reservaRepository.findByIdForUpdate(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        validarPropietarioOAdmin(reserva, obtenerUsuarioAutenticado());
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new PagoConflictException("Solo se puede pagar una reserva en estado PENDIENTE");
        }

        // Cobra el precio pactado al reservar, no el precio actual del servicio.
        BigDecimal monto = validarMonto(reserva.getPrecioPactado());
        Pago pago = pagoRepository.findByReservaIdForUpdate(idReserva).orElseGet(() ->
                pagoRepository.save(Pago.builder()
                        .reserva(reserva)
                        .monto(monto)
                        .metodoPago("online")
                        .estado(EstadoPago.pendiente)
                        .build()));

        if (pago.getMonto().compareTo(monto) != 0) {
            throw new PagoConflictException("El precio pactado no coincide con el monto fijado para el pago");
        }
        if (pago.getEstado() == EstadoPago.aprobado) {
            throw new PagoConflictException("La reserva ya tiene un pago aprobado");
        }

        // Reutiliza el intento pendiente para evitar abrir otro cobro simultáneo.
        PagoIntento pendiente = intentoRepository
                .findFirstByPago_IdPagoAndEstadoOrderByCreatedAtDesc(pago.getIdPago(), EstadoIntentoPago.pendiente)
                .orElse(null);
        if (pendiente != null) {
            return widget(pago, pendiente, true);
        }

        PagoIntento intento = intentoRepository.save(PagoIntento.builder()
                .pago(pago)
                .referencia("PAGO-" + UUID.randomUUID())
                .wompiEnvironment(configurationService.ambienteEventoEsperado())
                .estado(EstadoIntentoPago.pendiente)
                .build());
        pago.setEstado(EstadoPago.pendiente);
        pagoRepository.save(pago);
        return widget(pago, intento, false);
    }

    // Solo el dueño o un admin puede consultar el pago.
    @Transactional(readOnly = true)
    public PagoResponseDTO obtenerPorReserva(UUID idReserva) {
        Pago pago = pagoRepository.findByReserva_IdReserva(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado para la reserva"));
        validarPropietarioOAdmin(pago.getReserva(), obtenerUsuarioAutenticado());
        return response(pago);
    }

    // Acepta solo eventos Wompi del tipo y ambiente esperados.
    @Transactional
    public ResultadoEvento procesarEvento(JsonNode evento) {
        String tipo = evento.path("event").asString();
        if (!"transaction.updated".equals(tipo)) {
            log.info("Evento Wompi firmado ignorado: tipo={}", tipo.isBlank() ? "ausente" : tipo);
            return ResultadoEvento.IGNORADO;
        }
        String ambiente = textoRequerido(evento, "environment", "ambiente");
        if (!configurationService.ambienteEventoEsperado().equals(ambiente)) {
            log.warn("Evento Wompi firmado ignorado por ambiente no configurado: ambiente={}", ambiente);
            return ResultadoEvento.IGNORADO;
        }
        JsonNode transaction = evento.path("data").path("transaction");
        if (!transaction.isObject()) {
            throw new WompiEventoInvalidoException("El evento no contiene una transaccion valida");
        }
        return procesarTransaccion(transaction, ambiente);
    }

    // Solo un admin consulta en Wompi intentos pendientes conocidos.
    @Transactional
    public PagoResponseDTO reconciliar(UUID idReserva) {
        User usuario = obtenerUsuarioAutenticado();
        if (!esAdmin(usuario)) {
            throw new AccessDeniedException("Solo ADMIN puede reconciliar pagos con Wompi");
        }
        Pago pago = pagoRepository.findByReservaIdForUpdate(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado para la reserva"));
        List<PagoIntento> pendientes = intentoRepository
                .findByPago_IdPagoAndEstadoOrderByCreatedAtAsc(pago.getIdPago(), EstadoIntentoPago.pendiente)
                .stream()
                .filter(intento -> intento.getWompiTransactionId() != null && !intento.getWompiTransactionId().isBlank())
                .toList();
        if (pendientes.isEmpty()) {
            throw new PagoConflictException(
                    "No hay un intento pendiente con identificador de transaccion para consultar en Wompi");
        }

        for (PagoIntento intento : pendientes) {
            JsonNode transaction = transactionClient.consultar(intento.getWompiTransactionId());
            String reference = textoTransaccion(transaction, "reference", null);
            if (!intento.getReferencia().equals(reference)) {
                throw new WompiEventoInvalidoException("La transaccion consultada no coincide con el intento local");
            }
            PagoIntento bloqueado = intentoRepository.findByReferenciaForUpdate(reference)
                    .orElseThrow(() -> new PagoConflictException("El intento dejo de estar disponible"));
            aplicarTransaccion(bloqueado, leerTransaccion(transaction), configurationService.ambienteEventoEsperado());
        }
        return response(pago);
    }

    // Consulta la transacción en Wompi y compara ID y referencia.
    @Transactional
    public PagoResponseDTO verificar(UUID idReserva, String referencia, String transactionId) {
        if (referencia == null || referencia.isBlank() || referencia.length() > 100
                || transactionId == null || transactionId.isBlank() || transactionId.length() > 100) {
            throw new IllegalArgumentException("Referencia o identificador de transaccion no valido");
        }
        Pago pago = pagoRepository.findByReserva_IdReserva(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado para la reserva"));
        validarPropietarioOAdmin(pago.getReserva(), obtenerUsuarioAutenticado());
        intentoRepository.findByReferenciaAndPago_IdPago(referencia, pago.getIdPago())
                .orElseThrow(() -> new RecursoNoEncontradoException("Intento de pago no encontrado"));

        JsonNode transaction = transactionClient.consultar(transactionId);
        TransaccionWompi datos = leerTransaccion(transaction);
        if (!transactionId.equals(datos.id()) || !referencia.equals(datos.referencia())) {
            throw new WompiEventoInvalidoException("La transaccion consultada no coincide con el intento local");
        }
        PagoIntento intento = intentoRepository.findByReferenciaForUpdate(referencia)
                .orElseThrow(() -> new RecursoNoEncontradoException("Intento de pago no encontrado"));
        if (!intento.getPago().getIdPago().equals(pago.getIdPago())) {
            throw new AccessDeniedException("El intento no pertenece a esta reserva");
        }
        aplicarTransaccion(intento, datos, configurationService.ambienteEventoEsperado());
        return response(pago);
    }

    private ResultadoEvento procesarTransaccion(JsonNode transaction, String ambiente) {
        TransaccionWompi datos = leerTransaccion(transaction);
        PagoIntento intento = intentoRepository.findByReferenciaForUpdate(datos.referencia())
                .orElse(null);
        if (intento == null) {
            log.info("Evento Wompi firmado ignorado: referencia no registrada={}", datos.referencia());
            return ResultadoEvento.IGNORADO;
        }
        return aplicarTransaccion(intento, datos, ambiente);
    }

    private ResultadoEvento aplicarTransaccion(PagoIntento intento, TransaccionWompi datos, String ambiente) {
        // Estos datos vienen de Wompi; deben coincidir con el intento local antes de aprobar.
        if (!intento.getWompiEnvironment().equals(ambiente)) {
            throw new WompiEventoInvalidoException("El ambiente de la transaccion no coincide con el intento");
        }
        long esperado = montoEnCentavos(intento.getPago().getMonto());
        if (datos.montoEnCentavos() != esperado) {
            throw new WompiEventoInvalidoException("El monto recibido de Wompi no coincide con el pago registrado");
        }
        if (!COP.equals(datos.moneda())) {
            throw new WompiEventoInvalidoException("La moneda recibida de Wompi no es COP");
        }
        if (intento.getWompiTransactionId() != null
                && !intento.getWompiTransactionId().equals(datos.id())) {
            throw new PagoConflictException("El intento ya esta asociado a otra transaccion Wompi");
        }
        intentoRepository.findByWompiEnvironmentAndWompiTransactionId(ambiente, datos.id())
                .filter(asociado -> !asociado.getIdIntento().equals(intento.getIdIntento()))
                .ifPresent(asociado -> {
                    throw new PagoConflictException("La transaccion Wompi ya esta asociada a otro intento");
                });
        if (intento.getWompiPaymentMethodType() != null
                && !intento.getWompiPaymentMethodType().equals(datos.metodo())) {
            throw new PagoConflictException("El metodo Wompi no coincide con el registrado para el intento");
        }

        if (esEventoRepetido(intento, datos)) {
            return ResultadoEvento.DUPLICADO;
        }
        if (esIntentoAprobado(intento) && !"APPROVED".equals(datos.estado())) {
            log.warn("Evento Wompi tardio ignorado para intento aprobado: referencia={}, estado={}",
                    intento.getReferencia(), datos.estado());
            return ResultadoEvento.IGNORADO;
        }

        intento.setWompiTransactionId(datos.id());
        intento.setWompiPaymentMethodType(datos.metodo());
        Pago pago = pagoRepository.findByIdForUpdate(intento.getPago().getIdPago())
                .orElseThrow(() -> new PagoConflictException("El pago asociado al intento no existe"));

        switch (datos.estado()) {
            case "APPROVED" -> aprobar(intento, pago);
            case "DECLINED", "VOIDED", "ERROR" -> rechazar(intento, pago, datos.estado());
            case "PENDING" -> {
                if (intento.getEstado() != EstadoIntentoPago.pendiente) {
                    log.warn("Estado PENDING tardio ignorado: referencia={}", intento.getReferencia());
                    return ResultadoEvento.IGNORADO;
                }
                intento.setWompiStatus("PENDING");
            }
            default -> throw new WompiEventoInvalidoException("Estado Wompi no reconocido");
        }
        intentoRepository.save(intento);
        pagoRepository.save(pago);
        log.info("Intento Wompi actualizado: referencia={}, estadoWompi={}, estadoIntento={}, estadoPago={}",
                intento.getReferencia(), intento.getWompiStatus(), intento.getEstado(), pago.getEstado());
        return ResultadoEvento.PROCESADO;
    }

    private void aprobar(PagoIntento intento, Pago pago) {
        LocalDateTime ahora = LocalDateTime.now();
        intento.setWompiStatus("APPROVED");
        if (pago.getEstado() == EstadoPago.aprobado && intento.getEstado() != EstadoIntentoPago.aprobado) {
            intento.setEstado(EstadoIntentoPago.aprobado_duplicado);
            if (intento.getFechaConfirmacion() == null) {
                intento.setFechaConfirmacion(ahora);
            }
            log.error("Posible cobro duplicado Wompi: pago={}, intento={}, referencia={}",
                    pago.getIdPago(), intento.getIdIntento(), intento.getReferencia());
            return;
        }
        intento.setEstado(EstadoIntentoPago.aprobado);
        if (intento.getFechaConfirmacion() == null) {
            intento.setFechaConfirmacion(ahora);
        }
        pago.setEstado(EstadoPago.aprobado);
        if (pago.getFechaPago() == null) {
            pago.setFechaPago(ahora);
        }
        if (pago.getReserva().getEstado() == EstadoReserva.CANCELADA) {
            log.error("Pago aprobado para reserva cancelada: pago={}, reserva={}, intento={}",
                    pago.getIdPago(), pago.getReserva().getIdReserva(), intento.getIdIntento());
        }
    }

    private void rechazar(PagoIntento intento, Pago pago, String estadoWompi) {
        intento.setWompiStatus(estadoWompi);
        intento.setEstado(EstadoIntentoPago.rechazado);
        intento.setFechaConfirmacion(LocalDateTime.now());
        if (pago.getEstado() != EstadoPago.aprobado) {
            pago.setEstado(EstadoPago.rechazado);
        }
    }

    private boolean esEventoRepetido(PagoIntento intento, TransaccionWompi datos) {
        return datos.id().equals(intento.getWompiTransactionId())
                && datos.estado().equals(intento.getWompiStatus())
                && datos.metodo().equals(intento.getWompiPaymentMethodType());
    }

    private boolean esIntentoAprobado(PagoIntento intento) {
        return intento.getEstado() == EstadoIntentoPago.aprobado
                || intento.getEstado() == EstadoIntentoPago.aprobado_duplicado;
    }

    private TransaccionWompi leerTransaccion(JsonNode transaction) {
        String id = textoTransaccion(transaction, "id", null);
        String referencia = textoTransaccion(transaction, "reference", null);
        String moneda = textoTransaccion(transaction, "currency", null).toUpperCase(Locale.ROOT);
        String metodo = textoTransaccion(transaction, "payment_method_type", "paymentMethodType").toUpperCase(Locale.ROOT);
        String estado = textoTransaccion(transaction, "status", null).toUpperCase(Locale.ROOT);
        if (!ESTADOS_WOMPI.contains(estado)) {
            throw new WompiEventoInvalidoException("Estado Wompi no reconocido");
        }
        JsonNode amount = transaction.has("amount_in_cents")
                ? transaction.path("amount_in_cents") : transaction.path("amountInCents");
        if (!amount.isIntegralNumber() || amount.asLong(-1) <= 0) {
            throw new WompiEventoInvalidoException("El monto de la transaccion no es valido");
        }
        return new TransaccionWompi(id, referencia, amount.asLong(), moneda, metodo, estado);
    }

    private String textoTransaccion(JsonNode transaction, String snakeCase, String camelCase) {
        JsonNode value = transaction.path(snakeCase);
        if ((value.isMissingNode() || value.isNull()) && camelCase != null) {
            value = transaction.path(camelCase);
        }
        if (!value.isTextual() || value.asString().isBlank()) {
            throw new WompiEventoInvalidoException("La transaccion Wompi no contiene " + snakeCase);
        }
        return value.asString();
    }

    private String textoRequerido(JsonNode node, String field, String label) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asString().isBlank()) {
            throw new WompiEventoInvalidoException("El evento Wompi no contiene " + label);
        }
        return value.asString();
    }

    private PagoWidgetResponseDTO widget(Pago pago, PagoIntento intento, boolean reutilizado) {
        long centavos = montoEnCentavos(pago.getMonto());
        return new PagoWidgetResponseDTO(
                pago.getIdPago(), intento.getIdIntento(), pago.getReserva().getIdReserva(), intento.getReferencia(),
                centavos, COP, wompiProperties.getPublicKey(),
                signatureService.crearFirmaIntegridad(intento.getReferencia(), centavos, COP),
                frontendUrl + "/pagos/resultado", reutilizado);
    }

    private PagoResponseDTO response(Pago pago) {
        List<PagoIntentoResponseDTO> intentos = intentoRepository.findByPago_IdPagoOrderByCreatedAtAsc(pago.getIdPago())
                .stream().map(this::response).toList();
        PagoIntentoResponseDTO actual = intentos.isEmpty() ? null : intentos.get(intentos.size() - 1);
        return new PagoResponseDTO(pago.getIdPago(), pago.getReserva().getIdReserva(), pago.getMetodoPago(),
                pago.getMonto(), pago.getEstado().name(), pago.getFechaPago(), actual, intentos);
    }

    private PagoIntentoResponseDTO response(PagoIntento intento) {
        return new PagoIntentoResponseDTO(intento.getIdIntento(), intento.getReferencia(),
                intento.getWompiTransactionId(), intento.getWompiPaymentMethodType(), intento.getWompiStatus(),
                intento.getWompiEnvironment(), intento.getEstado().name(), intento.getFechaConfirmacion(),
                intento.getCreatedAt());
    }

    private BigDecimal validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0 || monto.stripTrailingZeros().scale() > 0) {
            throw new IllegalStateException("El precio del servicio debe ser un valor positivo entero en COP");
        }
        montoEnCentavos(monto);
        return monto;
    }

    private long montoEnCentavos(BigDecimal monto) {
        try {
            return monto.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalStateException("El monto del servicio no es valido", exception);
        }
    }

    private User obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        return accountAccessService.activeUser(auth.getName());
    }

    private void validarPropietarioOAdmin(Reserva reserva, User usuario) {
        if (!reserva.getUsuario().getUserId().equals(usuario.getUserId()) && !esAdmin(usuario)) {
            throw new AccessDeniedException("No tienes permiso para acceder al pago de esta reserva");
        }
    }

    private boolean esAdmin(User usuario) {
        return "ADMIN".equals(accountAccessService.currentRole(usuario));
    }

    private record TransaccionWompi(
            String id, String referencia, long montoEnCentavos, String moneda, String metodo, String estado) { }

    public enum ResultadoEvento {
        PROCESADO,
        DUPLICADO,
        IGNORADO
    }
}
