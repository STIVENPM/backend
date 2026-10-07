package com.lavarapido.backend_vehicular.reservas.service;

import com.lavarapido.backend_vehicular.asignaciones.repository.AsignacionRepository;
import com.lavarapido.backend_vehicular.reservas.dto.ReservaRequestDTO;
import com.lavarapido.backend_vehicular.reservas.dto.ReservaResponseDTO;
import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.reservas.repository.ReservaRepository;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
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

    private final ReservaService reservationService = new ReservaService(
            reservationRepository,
            userRepository,
            accountAccessService,
            vehicleRepository,
            serviceRepository,
            assignmentRepository
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
