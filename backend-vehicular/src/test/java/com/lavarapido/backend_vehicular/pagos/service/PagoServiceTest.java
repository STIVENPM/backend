package com.lavarapido.backend_vehicular.pagos.service;

import com.lavarapido.backend_vehicular.pagos.config.WompiConfigurationService;
import com.lavarapido.backend_vehicular.pagos.config.WompiProperties;
import com.lavarapido.backend_vehicular.pagos.dto.PagoResponseDTO;
import com.lavarapido.backend_vehicular.pagos.dto.PagoWidgetResponseDTO;
import com.lavarapido.backend_vehicular.pagos.entity.Pago;
import com.lavarapido.backend_vehicular.pagos.entity.PagoIntento;
import com.lavarapido.backend_vehicular.pagos.enums.EstadoIntentoPago;
import com.lavarapido.backend_vehicular.pagos.enums.EstadoPago;
import com.lavarapido.backend_vehicular.pagos.exception.PagoConflictException;
import com.lavarapido.backend_vehicular.pagos.exception.WompiEventoInvalidoException;
import com.lavarapido.backend_vehicular.pagos.exception.WompiProveedorException;
import com.lavarapido.backend_vehicular.pagos.repository.PagoIntentoRepository;
import com.lavarapido.backend_vehicular.pagos.repository.PagoRepository;
import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.reservas.repository.ReservaRepository;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PagoServiceTest {
    private static final UUID RESERVA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PAGO_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID INTENTO_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final String REFERENCIA = "PAGO-prueba-1";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PagoRepository pagoRepository;
    private PagoIntentoRepository intentoRepository;
    private ReservaRepository reservaRepository;
    private AccountAccessService accountAccessService;
    private WompiTransactionClient transactionClient;
    private WompiProperties properties;
    private WompiConfigurationService configurationService;
    private PagoService service;
    private User owner;
    private Reserva reserva;
    private Pago pago;
    private PagoIntento intento;

    @BeforeEach
    void setUp() {
        pagoRepository = mock(PagoRepository.class);
        intentoRepository = mock(PagoIntentoRepository.class);
        reservaRepository = mock(ReservaRepository.class);
        accountAccessService = mock(AccountAccessService.class);
        transactionClient = mock(WompiTransactionClient.class);

        properties = new WompiProperties();
        properties.setEnvironment("sandbox");
        properties.setPublicKey("pub_test_ficticia");
        properties.setPrivateKey("prv_test_ficticia");
        properties.setIntegritySecret("test_integrity_ficticio");
        properties.setEventsSecret("test_events_ficticio");
        configurationService = new WompiConfigurationService(properties);
        WompiSignatureService signatureService = new WompiSignatureService(properties, configurationService);
        service = new PagoService(pagoRepository, intentoRepository, reservaRepository,
                accountAccessService, properties, configurationService, signatureService, transactionClient);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173");

        owner = new User();
        owner.setUserId(OWNER_ID);
        owner.setEmail("owner@example.test");
        Servicio servicio = new Servicio();
        servicio.setPrecio(new BigDecimal("35000"));
        reserva = Reserva.builder()
                .idReserva(RESERVA_ID)
                .usuario(owner)
                .servicio(servicio)
                .precioPactado(new BigDecimal("35000"))
                .duracionMinutosPactada(60)
                .estado(EstadoReserva.PENDIENTE)
                .build();
        pago = Pago.builder()
                .idPago(PAGO_ID)
                .reserva(reserva)
                .metodoPago("online")
                .monto(new BigDecimal("35000"))
                .estado(EstadoPago.pendiente)
                .build();
        intento = PagoIntento.builder()
                .idIntento(INTENTO_ID)
                .pago(pago)
                .referencia(REFERENCIA)
                .wompiEnvironment("test")
                .estado(EstadoIntentoPago.pendiente)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner.getEmail(), null));
        when(accountAccessService.activeUser(owner.getEmail())).thenReturn(owner);
        when(intentoRepository.findByReferenciaForUpdate(REFERENCIA)).thenReturn(Optional.of(intento));
        when(intentoRepository.findByWompiEnvironmentAndWompiTransactionId(any(), any())).thenReturn(Optional.empty());
        when(pagoRepository.findByIdForUpdate(PAGO_ID)).thenReturn(Optional.of(pago));
        when(intentoRepository.save(any(PagoIntento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEQUI", "CARD", "PSE"})
    void aprobarAceptaMetodosHabilitadosSinAsignarReserva(String metodo) {
        PagoService.ResultadoEvento resultado = service.procesarEvento(evento("APPROVED", metodo, 3_500_000, "COP", "test"));

        assertEquals(PagoService.ResultadoEvento.PROCESADO, resultado);
        assertEquals(EstadoPago.aprobado, pago.getEstado());
        assertEquals(EstadoIntentoPago.aprobado, intento.getEstado());
        assertEquals(metodo, intento.getWompiPaymentMethodType());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        assertNotNull(pago.getFechaPago());
    }

    @Test
    void rechazaMontoIncorrecto() {
        assertThrows(WompiEventoInvalidoException.class,
                () -> service.procesarEvento(evento("APPROVED", "CARD", 100, "COP", "test")));
        assertEquals(EstadoPago.pendiente, pago.getEstado());
    }

    @Test
    void rechazaMonedaIncorrecta() {
        assertThrows(WompiEventoInvalidoException.class,
                () -> service.procesarEvento(evento("APPROVED", "CARD", 3_500_000, "USD", "test")));
        assertEquals(EstadoPago.pendiente, pago.getEstado());
    }

    @Test
    void ignoraAmbienteDistinto() {
        PagoService.ResultadoEvento resultado = service.procesarEvento(
                evento("APPROVED", "CARD", 3_500_000, "COP", "prod"));

        assertEquals(PagoService.ResultadoEvento.IGNORADO, resultado);
        verify(intentoRepository, never()).findByReferenciaForUpdate(any());
    }

    @Test
    void eventoDuplicadoEsIdempotenteYEventoTardioNoDegradaAprobado() {
        JsonNode aprobado = evento("APPROVED", "CARD", 3_500_000, "COP", "test");
        assertEquals(PagoService.ResultadoEvento.PROCESADO, service.procesarEvento(aprobado));
        var fechaPago = pago.getFechaPago();
        var fechaConfirmacion = intento.getFechaConfirmacion();

        assertEquals(PagoService.ResultadoEvento.DUPLICADO, service.procesarEvento(aprobado));
        assertEquals(PagoService.ResultadoEvento.IGNORADO,
                service.procesarEvento(evento("DECLINED", "CARD", 3_500_000, "COP", "test")));

        assertEquals(EstadoPago.aprobado, pago.getEstado());
        assertEquals(EstadoIntentoPago.aprobado, intento.getEstado());
        assertEquals("APPROVED", intento.getWompiStatus());
        assertEquals(fechaPago, pago.getFechaPago());
        assertEquals(fechaConfirmacion, intento.getFechaConfirmacion());
    }

    @Test
    void reintentoTrasRechazoConservaIntentoAnterior() {
        service.procesarEvento(evento("DECLINED", "PSE", 3_500_000, "COP", "test"));
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findByReservaIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(pago));
        when(intentoRepository.findFirstByPago_IdPagoAndEstadoOrderByCreatedAtDesc(PAGO_ID, EstadoIntentoPago.pendiente))
                .thenReturn(Optional.empty());
        when(intentoRepository.save(any(PagoIntento.class))).thenAnswer(invocation -> {
            PagoIntento nuevo = invocation.getArgument(0);
            nuevo.setIdIntento(UUID.randomUUID());
            return nuevo;
        });

        PagoWidgetResponseDTO nuevo = service.iniciar(RESERVA_ID);

        assertFalse(nuevo.reutilizado());
        assertNotEquals(REFERENCIA, nuevo.referencia());
        assertEquals(EstadoIntentoPago.rechazado, intento.getEstado());
        assertEquals(EstadoPago.pendiente, pago.getEstado());
    }

    @Test
    void inicioReutilizaIntentoPendienteBajoBloqueo() {
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findByReservaIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(pago));
        when(intentoRepository.findFirstByPago_IdPagoAndEstadoOrderByCreatedAtDesc(PAGO_ID, EstadoIntentoPago.pendiente))
                .thenReturn(Optional.of(intento));

        PagoWidgetResponseDTO response = service.iniciar(RESERVA_ID);

        assertTrue(response.reutilizado());
        assertEquals(REFERENCIA, response.referencia());
        verify(reservaRepository).findByIdForUpdate(RESERVA_ID);
        verify(intentoRepository, never()).save(any());
    }

    @Test
    void precioNuevoDelCatalogoNoCambiaElMontoPactado() {
        reserva.getServicio().setPrecio(new BigDecimal("99999"));
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findByReservaIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(pago));
        when(intentoRepository.findFirstByPago_IdPagoAndEstadoOrderByCreatedAtDesc(PAGO_ID, EstadoIntentoPago.pendiente))
                .thenReturn(Optional.of(intento));

        PagoWidgetResponseDTO response = service.iniciar(RESERVA_ID);

        assertEquals(3_500_000L, response.montoEnCentavos());
        assertEquals(new BigDecimal("35000"), reserva.getPrecioPactado());
    }

    @Test
    void inicioBloqueaNuevoIntentoCuandoPagoYaEstaAprobado() {
        pago.setEstado(EstadoPago.aprobado);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findByReservaIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(pago));

        assertThrows(PagoConflictException.class, () -> service.iniciar(RESERVA_ID));
        verify(intentoRepository, never()).save(any());
    }

    @Test
    void usuarioNoPuedeConsultarPagoDeOtraReserva() {
        User otro = new User();
        otro.setUserId(UUID.randomUUID());
        reserva.setUsuario(otro);
        when(pagoRepository.findByReserva_IdReserva(RESERVA_ID)).thenReturn(Optional.of(pago));

        assertThrows(AccessDeniedException.class, () -> service.obtenerPorReserva(RESERVA_ID));
    }

    @Test
    void aprobacionTardiaDeReservaCanceladaConservaPagoSinResucitarReserva() {
        reserva.setEstado(EstadoReserva.CANCELADA);

        service.procesarEvento(evento("APPROVED", "CARD", 3_500_000, "COP", "test"));

        assertEquals(EstadoPago.aprobado, pago.getEstado());
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }

    @Test
    void aprobacionDeSegundoIntentoDetectaPosibleCobroDuplicado() {
        pago.setEstado(EstadoPago.aprobado);

        service.procesarEvento(evento("APPROVED", "CARD", 3_500_000, "COP", "test"));

        assertEquals(EstadoIntentoPago.aprobado_duplicado, intento.getEstado());
        assertEquals(EstadoPago.aprobado, pago.getEstado());
    }

    @Test
    void reconciliacionAdminAplicaRespuestaVerificada() {
        autenticarAdmin();
        intento.setWompiTransactionId("tx-1");
        when(pagoRepository.findByReservaIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(pago));
        when(intentoRepository.findByPago_IdPagoAndEstadoOrderByCreatedAtAsc(PAGO_ID, EstadoIntentoPago.pendiente))
                .thenReturn(List.of(intento));
        when(transactionClient.consultar("tx-1"))
                .thenReturn(transaccion("APPROVED", "CARD", 3_500_000, "COP"));
        when(intentoRepository.findByPago_IdPagoOrderByCreatedAtAsc(PAGO_ID)).thenReturn(List.of(intento));

        PagoResponseDTO response = service.reconciliar(RESERVA_ID);

        assertEquals("aprobado", response.estado());
        assertEquals("APPROVED", response.intentoActual().wompiStatus());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
    }

    @Test
    void reconciliacionPropagaErrorDelProveedorSinMarcarFallo() {
        autenticarAdmin();
        intento.setWompiTransactionId("tx-1");
        when(pagoRepository.findByReservaIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(pago));
        when(intentoRepository.findByPago_IdPagoAndEstadoOrderByCreatedAtAsc(PAGO_ID, EstadoIntentoPago.pendiente))
                .thenReturn(List.of(intento));
        when(transactionClient.consultar("tx-1"))
                .thenThrow(new WompiProveedorException("timeout", new RuntimeException("timeout")));

        assertThrows(WompiProveedorException.class, () -> service.reconciliar(RESERVA_ID));
        assertEquals(EstadoIntentoPago.pendiente, intento.getEstado());
        assertEquals(EstadoPago.pendiente, pago.getEstado());
    }

    @Test
    void verificacionDesdeWidgetConsultaWompiAntesDeAprobar() {
        when(pagoRepository.findByReserva_IdReserva(RESERVA_ID)).thenReturn(Optional.of(pago));
        when(intentoRepository.findByReferenciaAndPago_IdPago(REFERENCIA, PAGO_ID)).thenReturn(Optional.of(intento));
        when(transactionClient.consultar("tx-1"))
                .thenReturn(transaccion("APPROVED", "CARD", 3_500_000, "COP"));
        when(intentoRepository.findByPago_IdPagoOrderByCreatedAtAsc(PAGO_ID)).thenReturn(List.of(intento));

        PagoResponseDTO response = service.verificar(RESERVA_ID, REFERENCIA, "tx-1");

        assertEquals("aprobado", response.estado());
        assertEquals("tx-1", intento.getWompiTransactionId());
        assertEquals("CARD", intento.getWompiPaymentMethodType());
    }

    @Test
    void verificacionRechazaReferenciaDistintaSinPersistir() {
        when(pagoRepository.findByReserva_IdReserva(RESERVA_ID)).thenReturn(Optional.of(pago));

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.verificar(RESERVA_ID, "PAGO-falsa", "tx-1"));
        assertEquals(EstadoPago.pendiente, pago.getEstado());
        verifyNoInteractions(transactionClient);
        verify(intentoRepository, never()).findByReferenciaForUpdate(any());
    }

    @Test
    void verificacionNoConsultaWompiParaReservaAjena() {
        User otro = new User();
        otro.setUserId(UUID.randomUUID());
        reserva.setUsuario(otro);
        when(pagoRepository.findByReserva_IdReserva(RESERVA_ID)).thenReturn(Optional.of(pago));

        assertThrows(AccessDeniedException.class,
                () -> service.verificar(RESERVA_ID, REFERENCIA, "tx-1"));
        verifyNoInteractions(transactionClient);
    }

    private void autenticarAdmin() {
        when(accountAccessService.currentRole(owner)).thenReturn("ADMIN");
    }

    private ObjectNode evento(String estado, String metodo, long monto, String moneda, String ambiente) {
        ObjectNode evento = objectMapper.createObjectNode();
        evento.put("event", "transaction.updated");
        evento.put("environment", ambiente);
        evento.set("data", objectMapper.createObjectNode().set("transaction", transaccion(estado, metodo, monto, moneda)));
        return evento;
    }

    private ObjectNode transaccion(String estado, String metodo, long monto, String moneda) {
        ObjectNode transaction = objectMapper.createObjectNode();
        transaction.put("id", "tx-1");
        transaction.put("reference", REFERENCIA);
        transaction.put("amount_in_cents", monto);
        transaction.put("currency", moneda);
        transaction.put("payment_method_type", metodo);
        transaction.put("status", estado);
        return transaction;
    }
}
