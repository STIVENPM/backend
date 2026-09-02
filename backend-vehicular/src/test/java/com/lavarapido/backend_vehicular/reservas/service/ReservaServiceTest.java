package com.lavarapido.backend_vehicular.reservas.service;

import com.lavarapido.backend_vehicular.reservas.dto.ReservaRequestDTO;
import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.reservas.repository.ReservaRepository;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
import com.lavarapido.backend_vehicular.servicios.repository.ServicioRepository;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.entity.UserRole;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.users.repository.UserRoleRepository;
import com.lavarapido.backend_vehicular.vehiculos.entity.Vehiculo;
import com.lavarapido.backend_vehicular.vehiculos.repository.VehiculoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private VehiculoRepository vehiculoRepository;
    @Mock private ServicioRepository servicioRepository;

    @InjectMocks private ReservaService reservaService;

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void crear_adminPuedeReservarParaVehiculoDeOtroUsuario() {
        User admin = usuario("admin@lavarapido.com");
        User propietario = usuario("cliente@lavarapido.com");
        Vehiculo vehiculo = vehiculo(propietario);
        Servicio servicio = servicio();
        autenticarComo(admin);

        UserRole rolAdmin = new UserRole();
        rolAdmin.setRole(new com.lavarapido.backend_vehicular.roles.entity.Role());
        rolAdmin.getRole().setRoleName("ADMIN");

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(userRoleRepository.findActiveRoleByUserId(admin.getUserId())).thenReturn(Optional.of(rolAdmin));
        when(vehiculoRepository.findById(vehiculo.getIdVehiculo())).thenReturn(Optional.of(vehiculo));
        when(servicioRepository.findById(servicio.getIdServicio())).thenReturn(Optional.of(servicio));
        when(reservaRepository.findSolapamientosPorVehiculo(any(), any(), any(), any(), eq("CANCELADA")))
                .thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reservaService.crear(request(vehiculo, servicio));

        ArgumentCaptor<Reserva> reservaCaptor = ArgumentCaptor.forClass(Reserva.class);
        org.mockito.Mockito.verify(reservaRepository).save(reservaCaptor.capture());
        assertThat(reservaCaptor.getValue().getUsuario()).isSameAs(propietario);
    }

    @Test
    void crear_userNoPuedeReservarParaVehiculoDeOtroUsuario() {
        User usuarioAutenticado = usuario("cliente-uno@lavarapido.com");
        User propietario = usuario("cliente-dos@lavarapido.com");
        Vehiculo vehiculo = vehiculo(propietario);
        Servicio servicio = servicio();
        autenticarComo(usuarioAutenticado);

        when(userRepository.findByEmail(usuarioAutenticado.getEmail())).thenReturn(Optional.of(usuarioAutenticado));
        when(userRoleRepository.findActiveRoleByUserId(usuarioAutenticado.getUserId())).thenReturn(Optional.empty());
        when(vehiculoRepository.findById(vehiculo.getIdVehiculo())).thenReturn(Optional.of(vehiculo));
        when(servicioRepository.findById(servicio.getIdServicio())).thenReturn(Optional.of(servicio));

        assertThatThrownBy(() -> reservaService.crear(request(vehiculo, servicio)))
                .isInstanceOf(com.lavarapido.backend_vehicular.reservas.exception.VehiculoNoPerteneceUsuarioException.class)
                .hasMessage("El vehículo no pertenece al usuario autenticado");
    }

    private void autenticarComo(User usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario.getEmail(), null));
    }

    private ReservaRequestDTO request(Vehiculo vehiculo, Servicio servicio) {
        return new ReservaRequestDTO(vehiculo.getIdVehiculo(), servicio.getIdServicio(),
                LocalDate.of(2026, 9, 10), LocalTime.of(10, 0));
    }

    private User usuario(String email) {
        User usuario = new User();
        usuario.setUserId(UUID.randomUUID());
        usuario.setEmail(email);
        usuario.setFirstName("Nombre");
        return usuario;
    }

    private Vehiculo vehiculo(User propietario) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setIdVehiculo(UUID.randomUUID());
        vehiculo.setUsuario(propietario);
        vehiculo.setPlaca("ABC123");
        return vehiculo;
    }

    private Servicio servicio() {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(UUID.randomUUID());
        servicio.setNombre("Lavado general");
        servicio.setPrecio(BigDecimal.valueOf(35000));
        servicio.setDuracionMinutos(60);
        return servicio;
    }
}
