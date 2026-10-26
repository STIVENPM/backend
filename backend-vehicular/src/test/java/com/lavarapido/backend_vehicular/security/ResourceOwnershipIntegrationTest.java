package com.lavarapido.backend_vehicular.security;

import com.lavarapido.backend_vehicular.asignaciones.controller.AsignacionController;
import com.lavarapido.backend_vehicular.asignaciones.entity.Asignacion;
import com.lavarapido.backend_vehicular.asignaciones.enums.EstadoAsignacion;
import com.lavarapido.backend_vehicular.asignaciones.repository.AsignacionRepository;
import com.lavarapido.backend_vehicular.asignaciones.service.AsignacionService;
import com.lavarapido.backend_vehicular.auditoria.service.AuditoriaService;
import com.lavarapido.backend_vehicular.marcas.entity.Marca;
import com.lavarapido.backend_vehicular.marcas.repository.MarcaRepository;
import com.lavarapido.backend_vehicular.notificaciones.service.NotificacionService;
import com.lavarapido.backend_vehicular.operadores.entity.Operador;
import com.lavarapido.backend_vehicular.operadores.repository.OperadorRepository;
import com.lavarapido.backend_vehicular.pagos.repository.PagoIntentoRepository;
import com.lavarapido.backend_vehicular.pagos.repository.PagoRepository;
import com.lavarapido.backend_vehicular.reservas.controller.ReservaController;
import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.reservas.repository.ReservaRepository;
import com.lavarapido.backend_vehicular.reservas.service.ReservaService;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
import com.lavarapido.backend_vehicular.servicios.repository.ServicioRepository;
import com.lavarapido.backend_vehicular.shared.config.SecurityConfig;
import com.lavarapido.backend_vehicular.shared.exception.GlobalExceptionHandler;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.vehiculos.controller.VehiculoController;
import com.lavarapido.backend_vehicular.vehiculos.entity.Vehiculo;
import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;
import com.lavarapido.backend_vehicular.vehiculos.repository.VehiculoRepository;
import com.lavarapido.backend_vehicular.vehiculos.service.VehiculoService;
import com.lavarapido.backend_vehicular.vehiculos.validation.PlacaVehiculoValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class ResourceOwnershipIntegrationTest {
    private final JwtService jwt = mock(JwtService.class);
    private final AccountAccessService access = mock(AccountAccessService.class);
    private final AuthRateLimitFilter rateLimit = new AuthRateLimitFilter();
    private final UserRepository users = mock(UserRepository.class);
    private final VehiculoRepository vehicles = mock(VehiculoRepository.class);
    private final MarcaRepository brands = mock(MarcaRepository.class);
    private final ReservaRepository reservations = mock(ReservaRepository.class);
    private final ServicioRepository services = mock(ServicioRepository.class);
    private final AsignacionRepository assignments = mock(AsignacionRepository.class);
    private final OperadorRepository operators = mock(OperadorRepository.class);
    private final NotificacionService notifications = mock(NotificacionService.class);
    private final PagoRepository payments = mock(PagoRepository.class);
    private final PagoIntentoRepository paymentAttempts = mock(PagoIntentoRepository.class);
    private final AuditoriaService auditService = mock(AuditoriaService.class);
    private final UUID reservationId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final User owner = user("owner@example.test");
    private final User other = user("other@example.test");
    private final User operatorUser = user("operator@example.test");
    private final User otherOperatorUser = user("other-operator@example.test");
    private final Marca brand = new Marca();
    private final Vehiculo vehicle = new Vehiculo();
    private final Servicio service = new Servicio();
    private final Reserva reservation = new Reserva();
    private final Operador operator = Operador.builder().idOperador(UUID.randomUUID()).usuario(operatorUser).estado(true).build();
    private final Operador otherOperator = Operador.builder().idOperador(UUID.randomUUID()).usuario(otherOperatorUser).estado(true).build();
    private final Asignacion assignment = Asignacion.builder().idAsignacion(assignmentId).reserva(reservation)
            .operador(operator).estado(EstadoAsignacion.asignada).build();
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        brand.setIdMarca(UUID.randomUUID());
        brand.setNombre("Test brand");
        brand.setEstado(true);
        vehicle.setIdVehiculo(vehicleId);
        vehicle.setUsuario(owner);
        vehicle.setMarca(brand);
        vehicle.setPlaca("ABC123");
        vehicle.setColor("Black");
        vehicle.setTipoVehiculo(TipoVehiculo.CARRO);
        vehicle.setEstado(true);
        service.setIdServicio(UUID.randomUUID());
        service.setNombre("Wash");
        service.setDescripcion("Test wash");
        reservation.setIdReserva(reservationId);
        reservation.setUsuario(owner);
        reservation.setVehiculo(vehicle);
        reservation.setServicio(service);
        reservation.setFechaReserva(LocalDate.now().plusDays(2));
        reservation.setHoraReserva(LocalTime.of(10, 0));
        reservation.setPrecioPactado(new BigDecimal("25000"));
        reservation.setDuracionMinutosPactada(60);
        reservation.setEstado(EstadoReserva.PENDIENTE);

        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.addBeanFactoryPostProcessor(factory -> {
            factory.registerSingleton("jwtService", jwt);
            factory.registerSingleton("accountAccessService", access);
            factory.registerSingleton("authRateLimitFilter", rateLimit);
            factory.registerSingleton("userRepository", users);
            factory.registerSingleton("vehiculoRepository", vehicles);
            factory.registerSingleton("marcaRepository", brands);
            factory.registerSingleton("reservaRepository", reservations);
            factory.registerSingleton("servicioRepository", services);
            factory.registerSingleton("asignacionRepository", assignments);
            factory.registerSingleton("operadorRepository", operators);
            factory.registerSingleton("notificacionService", notifications);
            factory.registerSingleton("pagoRepository", payments);
            factory.registerSingleton("pagoIntentoRepository", paymentAttempts);
            factory.registerSingleton("auditoriaService", auditService);
        });
        context.register(TestConfiguration.class);
        context.refresh();
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void reservationOwnerCanReadAndOtherClientCannot() throws Exception {
        when(reservations.findById(reservationId)).thenReturn(Optional.of(reservation));
        authenticate(owner, "USER");
        mvc.perform(get("/api/reservas/{id}", reservationId).header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva").value(reservationId.toString()));

        authenticate(other, "USER");
        mvc.perform(get("/api/reservas/{id}", reservationId).header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        verify(assignments, times(1)).findByReserva_IdReserva(reservationId);
    }

    @Test
    void otherClientCannotListOrCancelOwnersReservation() throws Exception {
        when(users.findById(owner.getUserId())).thenReturn(Optional.of(owner));
        when(reservations.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        authenticate(other, "USER");

        mvc.perform(get("/api/reservas/usuario/{id}", owner.getUserId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/reservas/{id}/cancelar", reservationId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        assertEquals(EstadoReserva.PENDIENTE, reservation.getEstado());
        verify(reservations, never()).save(any());
        verifyNoInteractions(payments, notifications);
    }

    @Test
    void ownerCanListOwnReservations() throws Exception {
        when(users.findById(owner.getUserId())).thenReturn(Optional.of(owner));
        when(reservations.findByUsuario_UserIdOrderByFechaReservaDescHoraReservaDesc(owner.getUserId()))
                .thenReturn(List.of(reservation));
        when(assignments.findByReserva_IdReservaIn(List.of(reservationId))).thenReturn(List.of());
        authenticate(owner, "USER");

        mvc.perform(get("/api/reservas/usuario/{id}", owner.getUserId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idReserva").value(reservationId.toString()));
    }

    @Test
    void ownerCanCancelReservation() throws Exception {
        when(reservations.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        when(reservations.save(reservation)).thenReturn(reservation);
        authenticate(owner, "USER");

        mvc.perform(patch("/api/reservas/{id}/cancelar", reservationId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        assertEquals(EstadoReserva.CANCELADA, reservation.getEstado());
        verify(reservations).save(reservation);
    }

    @Test
    void administratorCannotUseCustomerCancellationEndpoint() throws Exception {
        authenticate(owner, "ADMIN");

        mvc.perform(patch("/api/reservas/{id}/cancelar", reservationId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        assertEquals(EstadoReserva.PENDIENTE, reservation.getEstado());
        verifyNoInteractions(payments, notifications);
        verify(reservations, never()).save(any());
    }

    @Test
    void administrativeStateEndpointRejectsCancelledState() throws Exception {
        authenticate(owner, "ADMIN");

        mvc.perform(patch("/api/reservas/{id}/estado", reservationId)
                        .param("estado", "CANCELADA")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest());
        assertEquals(EstadoReserva.PENDIENTE, reservation.getEstado());
        verify(reservations, never()).save(any());
    }

    @Test
    void ownerCannotCancelAssignedReservation() throws Exception {
        reservation.setEstado(EstadoReserva.ASIGNADA);
        when(reservations.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        authenticate(owner, "USER");

        mvc.perform(patch("/api/reservas/{id}/cancelar", reservationId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVA_NO_PENDIENTE"));
        assertEquals(EstadoReserva.ASIGNADA, reservation.getEstado());
        verifyNoInteractions(payments, notifications);
        verify(reservations, never()).save(any());
    }

    @Test
    void clientCannotCreateReservationWithAnotherClientsVehicle() throws Exception {
        when(vehicles.findByIdForUpdate(vehicleId)).thenReturn(Optional.of(vehicle));
        when(services.findByIdForUpdate(service.getIdServicio())).thenReturn(Optional.of(service));
        authenticate(other, "USER");

        mvc.perform(post("/api/reservas").header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"fkIdVehiculo":"%s","fkIdServicio":"%s","fechaReserva":"%s","horaReserva":"10:00:00"}
                                """.formatted(vehicleId, service.getIdServicio(), reservation.getFechaReserva())))
                .andExpect(status().isForbidden());
        verify(reservations, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void clientCanListAndEditOwnVehicleButNotAnotherClientsVehicle() throws Exception {
        when(vehicles.findByUsuario_UserIdAndEstadoTrue(owner.getUserId())).thenReturn(List.of(vehicle));
        when(vehicles.findByUsuario_UserIdAndEstadoTrue(other.getUserId())).thenReturn(List.of());
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(brands.findById(brand.getIdMarca())).thenReturn(Optional.of(brand));
        when(vehicles.save(vehicle)).thenReturn(vehicle);
        authenticate(owner, "USER");

        mvc.perform(get("/api/vehiculos/mis-vehiculos").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idVehiculo").value(vehicleId.toString()));
        mvc.perform(put("/api/vehiculos/{id}", vehicleId).header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleBody("Blue")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.color").value("Blue"));

        authenticate(other, "USER");
        mvc.perform(get("/api/vehiculos/mis-vehiculos").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(put("/api/vehiculos/{id}", vehicleId).header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleBody("Red")))
                .andExpect(status().isForbidden());
        assertEquals("Blue", vehicle.getColor());
        verify(vehicles).save(vehicle);
    }

    @Test
    void otherClientCannotReadOrChangeVehicleState() throws Exception {
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        authenticate(other, "USER");

        mvc.perform(get("/api/vehiculos/{id}", vehicleId).header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/vehiculos/{id}/estado", vehicleId).param("activo", "false")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        assertEquals(true, vehicle.getEstado());
        verify(vehicles, never()).save(any());
    }

    @Test
    void ownerCanChangeVehicleState() throws Exception {
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicles.save(vehicle)).thenReturn(vehicle);
        authenticate(owner, "USER");

        mvc.perform(patch("/api/vehiculos/{id}/estado", vehicleId).param("activo", "false")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(false));
        assertEquals(false, vehicle.getEstado());
    }

    @Test
    void operatorSeesOnlyOwnAssignmentsAndCannotUpdateAnotherOperatorsAssignment() throws Exception {
        when(users.findByEmail(operatorUser.getEmail())).thenReturn(Optional.of(operatorUser));
        when(operators.findByUsuario_UserId(operatorUser.getUserId())).thenReturn(Optional.of(operator));
        when(assignments.findByOperador_IdOperadorOrderByFechaAsignacionDesc(operator.getIdOperador()))
                .thenReturn(List.of(assignment));
        when(users.findByEmail(otherOperatorUser.getEmail())).thenReturn(Optional.of(otherOperatorUser));
        when(operators.findByUsuario_UserId(otherOperatorUser.getUserId())).thenReturn(Optional.of(otherOperator));
        when(assignments.findByOperador_IdOperadorOrderByFechaAsignacionDesc(otherOperator.getIdOperador()))
                .thenReturn(List.of());
        when(assignments.findById(assignmentId)).thenReturn(Optional.of(assignment));
        authenticate(operatorUser, "OPERATOR");

        mvc.perform(get("/api/asignaciones/mis-asignaciones").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idAsignacion").value(assignmentId.toString()));
        authenticate(otherOperatorUser, "OPERATOR");
        mvc.perform(get("/api/asignaciones/mis-asignaciones").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(patch("/api/asignaciones/{id}/estado", assignmentId)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"en_proceso\"}"))
                .andExpect(status().isForbidden());
        assertEquals(EstadoAsignacion.asignada, assignment.getEstado());
        verify(assignments, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void operatorCannotSkipAssignmentTransition() throws Exception {
        when(users.findByEmail(operatorUser.getEmail())).thenReturn(Optional.of(operatorUser));
        when(operators.findByUsuario_UserId(operatorUser.getUserId())).thenReturn(Optional.of(operator));
        when(assignments.findById(assignmentId)).thenReturn(Optional.of(assignment));
        authenticate(operatorUser, "OPERATOR");

        mvc.perform(patch("/api/asignaciones/{id}/estado", assignmentId)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"completada\"}"))
                .andExpect(status().isConflict());
        assertEquals(EstadoAsignacion.asignada, assignment.getEstado());
        verify(assignments, never()).save(any());
    }

    @Test
    void operatorCannotCancelReservationThroughAssignmentState() throws Exception {
        reservation.setEstado(EstadoReserva.ASIGNADA);
        when(users.findByEmail(operatorUser.getEmail())).thenReturn(Optional.of(operatorUser));
        when(operators.findByUsuario_UserId(operatorUser.getUserId())).thenReturn(Optional.of(operator));
        when(assignments.findById(assignmentId)).thenReturn(Optional.of(assignment));
        authenticate(operatorUser, "OPERATOR");

        mvc.perform(patch("/api/asignaciones/{id}/estado", assignmentId)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"cancelada\"}"))
                .andExpect(status().isConflict());
        assertEquals(EstadoAsignacion.asignada, assignment.getEstado());
        assertEquals(EstadoReserva.ASIGNADA, reservation.getEstado());
        verify(assignments, never()).save(any());
        verify(reservations, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void assignedOperatorCanStartOwnAssignment() throws Exception {
        reservation.setEstado(EstadoReserva.ASIGNADA);
        when(users.findByEmail(operatorUser.getEmail())).thenReturn(Optional.of(operatorUser));
        when(operators.findByUsuario_UserId(operatorUser.getUserId())).thenReturn(Optional.of(operator));
        when(assignments.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(assignments.save(assignment)).thenReturn(assignment);
        when(reservations.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        when(reservations.save(reservation)).thenReturn(reservation);
        authenticate(operatorUser, "OPERATOR");

        mvc.perform(patch("/api/asignaciones/{id}/estado", assignmentId)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"en_proceso\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("en_proceso"));
        assertEquals(EstadoAsignacion.en_proceso, assignment.getEstado());
        assertEquals(EstadoReserva.EN_PROCESO, reservation.getEstado());
        verify(assignments).save(assignment);
    }

    private void authenticate(User user, String role) {
        when(jwt.isTokenValid("token")).thenReturn(true);
        when(jwt.extractUserId("token")).thenReturn(user.getUserId());
        when(jwt.extractEmail("token")).thenReturn(user.getEmail());
        when(access.activeUser(user.getUserId())).thenReturn(user);
        when(access.activeUser(user.getEmail())).thenReturn(user);
        when(access.currentRole(user)).thenReturn(role);
    }

    private String vehicleBody(String color) {
        return """
                {"placa":"ABC123","color":"%s","tipoVehiculo":"CARRO","fkIdMarca":"%s"}
                """.formatted(color, brand.getIdMarca());
    }

    private static User user(String email) {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail(email);
        user.setFirstName("Test");
        user.setStatus(true);
        return user;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, ReservaService.class, ReservaController.class,
            VehiculoService.class, VehiculoController.class, PlacaVehiculoValidator.class,
            AsignacionService.class, AsignacionController.class, GlobalExceptionHandler.class})
    static class TestConfiguration {
        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, AccountAccessService access) {
            return new JwtAuthenticationFilter(jwtService, access);
        }
    }
}
