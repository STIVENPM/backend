package com.lavarapido.backend_vehicular.reservas.service;

import com.lavarapido.backend_vehicular.asignaciones.repository.AsignacionRepository;
import com.lavarapido.backend_vehicular.asignaciones.entity.Asignacion;
import com.lavarapido.backend_vehicular.asignaciones.enums.EstadoAsignacion;
import com.lavarapido.backend_vehicular.operadores.entity.Operador;
import com.lavarapido.backend_vehicular.reservas.dto.ReservaRequestDTO;
import com.lavarapido.backend_vehicular.reservas.dto.ReservaResponseDTO;
import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.reservas.exception.ReservaCancelacionConflictException;
import com.lavarapido.backend_vehicular.reservas.repository.ReservaRepository;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.notificaciones.service.NotificacionService;
import com.lavarapido.backend_vehicular.pagos.entity.Pago;
import com.lavarapido.backend_vehicular.pagos.entity.PagoIntento;
import com.lavarapido.backend_vehicular.pagos.enums.EstadoIntentoPago;
import com.lavarapido.backend_vehicular.pagos.enums.EstadoPago;
import com.lavarapido.backend_vehicular.pagos.repository.PagoIntentoRepository;
import com.lavarapido.backend_vehicular.pagos.repository.PagoRepository;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
import com.lavarapido.backend_vehicular.servicios.repository.ServicioRepository;
import com.lavarapido.backend_vehicular.shared.exception.GlobalExceptionHandler;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.vehiculos.entity.Vehiculo;
import com.lavarapido.backend_vehicular.vehiculos.repository.VehiculoRepository;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReservaServiceTest {
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID SERVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final LocalDate RESERVATION_DATE = LocalDate.of(2030, 5, 20);
    private static final LocalTime RESERVATION_TIME = LocalTime.of(9, 0);

    private final ReservaRepository reservationRepository = mock(ReservaRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AccountAccessService accountAccessService = mock(AccountAccessService.class);
    private final VehiculoRepository vehicleRepository = mock(VehiculoRepository.class);
    private final ServicioRepository serviceRepository = mock(ServicioRepository.class);
    private final AsignacionRepository assignmentRepository = mock(AsignacionRepository.class);
    private final NotificacionService notificationService = mock(NotificacionService.class);
    private final PagoRepository paymentRepository = mock(PagoRepository.class);
    private final PagoIntentoRepository paymentAttemptRepository = mock(PagoIntentoRepository.class);

    private final ReservaService reservationService = new ReservaService(
            reservationRepository,
            userRepository,
            accountAccessService,
            vehicleRepository,
            serviceRepository,
            assignmentRepository,
            notificationService,
            paymentRepository,
            paymentAttemptRepository
    );

    private User user;
    private Vehiculo vehicle;
    private Servicio service;
    private ReservaRequestDTO request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(USER_ID);
        user.setEmail("customer@example.test");
        user.setFirstName("Test");
        user.setLastName("Customer");
        user.setStatus(true);

        vehicle = new Vehiculo();
        vehicle.setIdVehiculo(VEHICLE_ID);
        vehicle.setUsuario(user);
        vehicle.setPlaca("ABC123");
        vehicle.setEstado(true);

        service = new Servicio();
        service.setIdServicio(SERVICE_ID);
        service.setNombre("Lavado de prueba");
        service.setPrecio(new BigDecimal("25000"));
        service.setDuracionMinutos(60);
        service.setEstado(true);

        request = new ReservaRequestDTO(VEHICLE_ID, SERVICE_ID, RESERVATION_DATE, RESERVATION_TIME);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), "unused")
        );
        when(accountAccessService.activeUser(user.getEmail())).thenReturn(user);
        when(accountAccessService.currentRole(user)).thenReturn("USER");
        when(vehicleRepository.findByIdForUpdate(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(serviceRepository.findByIdForUpdate(SERVICE_ID)).thenReturn(Optional.of(service));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsInactiveVehicleWithConflictAndDoesNotSaveReservation() {
        vehicle.setEstado(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reservationService.crear(request)
        );

        assertConflictResponse(exception);
        verify(vehicleRepository).findByIdForUpdate(VEHICLE_ID);
        verify(serviceRepository).findByIdForUpdate(SERVICE_ID);
        verify(reservationRepository, never()).save(any(Reserva.class));
    }

    @Test
    void rejectsInactiveServiceWithConflictAndDoesNotSaveReservation() {
        service.setEstado(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reservationService.crear(request)
        );

        assertConflictResponse(exception);
        verify(vehicleRepository).findByIdForUpdate(VEHICLE_ID);
        verify(serviceRepository).findByIdForUpdate(SERVICE_ID);
        verify(reservationRepository, never()).save(any(Reserva.class));
    }

    @Test
    void createsReservationWhenVehicleAndServiceAreActive() {
        when(reservationRepository.findSolapamientosPorVehiculo(
                VEHICLE_ID,
                RESERVATION_DATE,
                RESERVATION_TIME,
                LocalTime.of(10, 0),
                EstadoReserva.CANCELADA.name()
        )).thenReturn(List.of());
        when(reservationRepository.save(any(Reserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservaResponseDTO result = reservationService.crear(request);

        assertNotNull(result);
        assertEquals(VEHICLE_ID, result.getIdVehiculo());
        assertEquals(SERVICE_ID, result.getIdServicio());
        assertEquals(new BigDecimal("25000"), result.getPrecioServicio());
        assertEquals(60, result.getDuracionServicio());
        assertEquals(EstadoReserva.PENDIENTE, result.getEstado());

        var lockOrder = inOrder(vehicleRepository, serviceRepository);
        lockOrder.verify(vehicleRepository).findByIdForUpdate(VEHICLE_ID);
        lockOrder.verify(serviceRepository).findByIdForUpdate(SERVICE_ID);
        verify(reservationRepository).save(any(Reserva.class));
    }

    @Test
    void bothResourceLookupsUsePessimisticWriteLocks() throws NoSuchMethodException {
        Lock vehicleLock = VehiculoRepository.class
                .getMethod("findByIdForUpdate", UUID.class)
                .getAnnotation(Lock.class);
        Lock serviceLock = ServicioRepository.class
                .getMethod("findByIdForUpdate", UUID.class)
                .getAnnotation(Lock.class);

        assertNotNull(vehicleLock);
        assertNotNull(serviceLock);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, vehicleLock.value());
        assertEquals(LockModeType.PESSIMISTIC_WRITE, serviceLock.value());
    }

    @Test
    void repetirEstadoNoGeneraNotificacion() {
        UUID reservaId = UUID.randomUUID();
        Reserva reserva = reserva(reservaId, EstadoReserva.ASIGNADA);
        when(reservationRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        reservationService.cambiarEstado(reservaId, EstadoReserva.ASIGNADA);
        verify(reservationRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void cancelarReservaAsignadaCancelaAsignacionYNotificaAlPropietario() {
        UUID reservaId = UUID.randomUUID();
        Reserva reserva = reserva(reservaId, EstadoReserva.ASIGNADA);
        Asignacion asignacion = Asignacion.builder().reserva(reserva).estado(EstadoAsignacion.asignada).build();
        Operador operador = new Operador(); operador.setUsuario(user); asignacion.setOperador(operador);
        when(reservationRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(assignmentRepository.findByReserva_IdReserva(reservaId)).thenReturn(Optional.of(asignacion));
        when(reservationRepository.save(reserva)).thenReturn(reserva);
        reservationService.cambiarEstado(reservaId, EstadoReserva.CANCELADA);
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        assertEquals(EstadoAsignacion.cancelada, asignacion.getEstado());
        verify(assignmentRepository).save(asignacion);
        verify(notificationService).crear(user, "reserva:" + reservaId + ":CANCELADA",
                "booking", "Reserva cancelada", "Reserva cancelada.", reservaId, null);
    }

    @Test
    void cancelaReservaPendienteSinPagoYBloqueaReservaAntesDeConsultarPago() {
        UUID reservaId = UUID.randomUUID();
        Reserva reserva = reserva(reservaId, EstadoReserva.PENDIENTE);
        when(reservationRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(paymentRepository.findByReservaIdForUpdate(reservaId)).thenReturn(Optional.empty());
        when(reservationRepository.save(reserva)).thenReturn(reserva);

        reservationService.cancelar(reservaId);

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        verify(notificationService).crear(user, "reserva:" + reservaId + ":CANCELADA",
                "booking", "Reserva cancelada", "Reserva cancelada.", reservaId, null);
        var lockOrder = inOrder(reservationRepository, paymentRepository);
        lockOrder.verify(reservationRepository).findByIdForUpdate(reservaId);
        lockOrder.verify(paymentRepository).findByReservaIdForUpdate(reservaId);
    }

    @Test
    void rechazaCancelacionSiElPagoEstaAprobado() {
        UUID reservaId = UUID.randomUUID();
        Reserva reserva = reserva(reservaId, EstadoReserva.PENDIENTE);
        Pago pago = pago(reserva, EstadoPago.aprobado);
        when(reservationRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(paymentRepository.findByReservaIdForUpdate(reservaId)).thenReturn(Optional.of(pago));

        ReservaCancelacionConflictException exception = assertThrows(
                ReservaCancelacionConflictException.class,
                () -> reservationService.cancelar(reservaId)
        );

        assertEquals("PAGO_APROBADO", exception.getCode());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        verify(reservationRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void noConfirmaCancelacionConIntentoWompiPendiente() {
        UUID reservaId = UUID.randomUUID();
        Reserva reserva = reserva(reservaId, EstadoReserva.PENDIENTE);
        Pago pago = pago(reserva, EstadoPago.pendiente);
        PagoIntento intento = intento(pago, EstadoIntentoPago.pendiente, "PENDING");
        when(reservationRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(paymentRepository.findByReservaIdForUpdate(reservaId)).thenReturn(Optional.of(pago));
        when(paymentAttemptRepository.findByPago_IdPagoOrderByCreatedAtAsc(pago.getIdPago()))
                .thenReturn(List.of(intento));

        ReservaCancelacionConflictException exception = assertThrows(
                ReservaCancelacionConflictException.class,
                () -> reservationService.cancelar(reservaId)
        );

        assertEquals("PAGO_PENDIENTE", exception.getCode());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        verify(reservationRepository, never()).save(any(Reserva.class));
    }

    @Test
    void permiteCancelarTrasRechazoTerminalConfirmadoPorWompi() {
        UUID reservaId = UUID.randomUUID();
        Reserva reserva = reserva(reservaId, EstadoReserva.PENDIENTE);
        Pago pago = pago(reserva, EstadoPago.rechazado);
        PagoIntento intento = intento(pago, EstadoIntentoPago.rechazado, "DECLINED");
        when(reservationRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(paymentRepository.findByReservaIdForUpdate(reservaId)).thenReturn(Optional.of(pago));
        when(paymentAttemptRepository.findByPago_IdPagoOrderByCreatedAtAsc(pago.getIdPago()))
                .thenReturn(List.of(intento));
        when(reservationRepository.save(reserva)).thenReturn(reserva);

        reservationService.cancelar(reservaId);

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }

    @Test
    void noAceptaPagoRechazadoSinEstadoWompiFinalConocido() {
        UUID reservaId = UUID.randomUUID();
        Reserva reserva = reserva(reservaId, EstadoReserva.PENDIENTE);
        Pago pago = pago(reserva, EstadoPago.rechazado);
        PagoIntento intento = intento(pago, EstadoIntentoPago.rechazado, null);
        when(reservationRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(paymentRepository.findByReservaIdForUpdate(reservaId)).thenReturn(Optional.of(pago));
        when(paymentAttemptRepository.findByPago_IdPagoOrderByCreatedAtAsc(pago.getIdPago()))
                .thenReturn(List.of(intento));

        ReservaCancelacionConflictException exception = assertThrows(
                ReservaCancelacionConflictException.class,
                () -> reservationService.cancelar(reservaId)
        );

        assertEquals("PAGO_PENDIENTE", exception.getCode());
    }

    @Test
    void cancelacionRepetidaEsIdempotenteYNoDuplicaNotificacion() {
        UUID reservaId = UUID.randomUUID();
        Reserva reserva = reserva(reservaId, EstadoReserva.CANCELADA);
        when(reservationRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

        reservationService.cancelar(reservaId);
        reservationService.cancelar(reservaId);

        verify(paymentRepository, never()).findByReservaIdForUpdate(reservaId);
        verify(notificationService, never()).crear(any(), any(), any(), any(), any(), any(), any());
        verify(reservationRepository, never()).save(any(Reserva.class));
    }

    @Test
    void cancelacionDeReservaAjenaEsDenegadaAntesDeConsultarPago() {
        UUID reservaId = UUID.randomUUID();
        User otro = new User();
        otro.setUserId(UUID.randomUUID());
        Reserva reserva = reserva(reservaId, EstadoReserva.PENDIENTE);
        reserva.setUsuario(otro);
        when(reservationRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> reservationService.cancelar(reservaId));

        verify(paymentRepository, never()).findByReservaIdForUpdate(reservaId);
    }

    @Test
    void cancelacionDeReservaInexistenteDevuelve404() {
        UUID reservaId = UUID.randomUUID();
        when(reservationRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.empty());

        assertThrows(com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException.class,
                () -> reservationService.cancelar(reservaId));
        verifyNoInteractions(paymentRepository);
    }

    private Pago pago(Reserva reserva, EstadoPago estado) {
        return Pago.builder().idPago(UUID.randomUUID()).reserva(reserva)
                .monto(new BigDecimal("25000")).estado(estado).build();
    }

    private PagoIntento intento(Pago pago, EstadoIntentoPago estado, String estadoWompi) {
        return PagoIntento.builder().idIntento(UUID.randomUUID()).pago(pago)
                .referencia("PAGO-prueba").wompiStatus(estadoWompi)
                .estado(estado).build();
    }

    private Reserva reserva(UUID id, EstadoReserva estado) {
        return Reserva.builder().idReserva(id).usuario(user).vehiculo(vehicle).servicio(service)
                .fechaReserva(RESERVATION_DATE).horaReserva(RESERVATION_TIME)
                .precioPactado(new BigDecimal("25000")).duracionMinutosPactada(60).estado(estado).build();
    }

    private void assertConflictResponse(IllegalStateException exception) {
        ResponseEntity<java.util.Map<String, String>> response =
                new GlobalExceptionHandler().handleIllegalState(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(
                "La operacion entra en conflicto con el estado actual",
                response.getBody().get("error")
        );
    }
}
