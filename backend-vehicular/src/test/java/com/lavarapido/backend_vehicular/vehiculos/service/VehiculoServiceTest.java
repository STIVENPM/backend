package com.lavarapido.backend_vehicular.vehiculos.service;

import com.lavarapido.backend_vehicular.marcas.entity.Marca;
import com.lavarapido.backend_vehicular.marcas.repository.MarcaRepository;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.vehiculos.dto.VehiculoRequestDTO;
import com.lavarapido.backend_vehicular.vehiculos.entity.Vehiculo;
import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;
import com.lavarapido.backend_vehicular.vehiculos.repository.VehiculoRepository;
import com.lavarapido.backend_vehicular.vehiculos.validation.PlacaVehiculoValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VehiculoServiceTest {

    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID BRAND_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");

    private final VehiculoRepository vehicleRepository = mock(VehiculoRepository.class);
    private final MarcaRepository brandRepository = mock(MarcaRepository.class);
    private final AccountAccessService accountAccessService = mock(AccountAccessService.class);
    private final VehiculoService service = new VehiculoService(vehicleRepository, brandRepository,
            accountAccessService, new PlacaVehiculoValidator());

    private User user;
    private Marca brand;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000200"));
        user.setEmail("customer@example.test");
        user.setFirstName("Customer");
        user.setStatus(true);
        brand = new Marca();
        brand.setIdMarca(BRAND_ID);
        brand.setNombre("Marca de prueba");
        brand.setEstado(true);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null));
        when(accountAccessService.activeUser(user.getEmail())).thenReturn(user);
        when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.of(brand));
        when(vehicleRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsVehicleWithTrimmedUppercasePlate() {
        VehiculoRequestDTO request = new VehiculoRequestDTO(" abc123 ", "Negro", TipoVehiculo.CARRO, BRAND_ID);
        when(vehicleRepository.existsByPlaca("ABC123")).thenReturn(false);

        var response = service.crear(request);

        assertEquals("ABC123", response.placa());
        assertEquals(TipoVehiculo.CARRO, response.tipoVehiculo());
        verify(vehicleRepository).existsByPlaca("ABC123");
    }

    @Test
    void updatesTypeAndPlateOnlyWhenTheirCombinationIsValid() {
        Vehiculo existing = vehicle("ABC123", TipoVehiculo.CARRO);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(existing));
        when(vehicleRepository.existsByPlaca("ABC12D")).thenReturn(false);

        var response = service.actualizar(VEHICLE_ID,
                new VehiculoRequestDTO(" abc12d ", "Rojo", TipoVehiculo.MOTO, BRAND_ID));

        assertEquals("ABC12D", response.placa());
        assertEquals(TipoVehiculo.MOTO, response.tipoVehiculo());
    }

    @Test
    void preservesDuplicatePlateConflictDuringCreation() {
        when(vehicleRepository.existsByPlaca("ABC123")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.crear(new VehiculoRequestDTO("ABC123", "Negro", TipoVehiculo.CARRO, BRAND_ID)));
    }

    @Test
    void preservesDuplicatePlateConflictDuringUpdate() {
        Vehiculo existing = vehicle("ABC123", TipoVehiculo.CARRO);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(existing));
        when(vehicleRepository.existsByPlaca("DEF456")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.actualizar(VEHICLE_ID,
                        new VehiculoRequestDTO("DEF456", "Negro", TipoVehiculo.CARRO, BRAND_ID)));
    }

    private Vehiculo vehicle(String placa, TipoVehiculo tipo) {
        Vehiculo vehicle = new Vehiculo();
        vehicle.setIdVehiculo(VEHICLE_ID);
        vehicle.setUsuario(user);
        vehicle.setMarca(brand);
        vehicle.setPlaca(placa);
        vehicle.setTipoVehiculo(tipo);
        vehicle.setEstado(true);
        return vehicle;
    }
}
