package com.lavarapido.backend_vehicular.servicios.service;

import com.lavarapido.backend_vehicular.servicios.dto.ServicioRequestDTO;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
import com.lavarapido.backend_vehicular.servicios.exception.DuplicateServiceNameException;
import com.lavarapido.backend_vehicular.servicios.repository.ServicioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioServiceTest {
    private final ServicioRepository repository = mock(ServicioRepository.class);
    private final ServicioService service = new ServicioService(repository);

    @Test
    void createsValidServiceWithLargePriceAndShortDuration() {
        var request = new ServicioRequestDTO(
                "Lavado Premium", "sasasasasa", new BigDecimal("500005"), 27);
        when(repository.existsByNombre("Lavado Premium")).thenReturn(false);
        when(repository.save(any(Servicio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.crear(request);

        assertEquals("Lavado Premium", response.getNombre());
        assertEquals(new BigDecimal("500005"), response.getPrecio());
        assertEquals(27, response.getDuracionMinutos());
        verify(repository).save(any(Servicio.class));
    }

    @Test
    void rejectsDuplicateServiceNameWithSpecificException() {
        var request = new ServicioRequestDTO(
                "Lavado Premium", "sasasasasa", new BigDecimal("500005"), 27);
        when(repository.existsByNombre("Lavado Premium")).thenReturn(true);

        assertThrows(DuplicateServiceNameException.class, () -> service.crear(request));
    }

    @Test
    void listsActiveAndInactiveServices() {
        Servicio activeService = service(UUID.randomUUID(), "Active service", true);
        Servicio inactiveService = service(UUID.randomUUID(), "Inactive service", false);
        when(repository.findAll()).thenReturn(List.of(activeService, inactiveService));

        var response = service.listarTodos();

        assertEquals(2, response.size());
        assertEquals(true, response.get(0).getEstado());
        assertEquals(false, response.get(1).getEstado());
    }

    @Test
    void changesServiceStateWithoutDeletingTheService() {
        UUID serviceId = UUID.randomUUID();
        Servicio existingService = service(serviceId, "Service", false);
        when(repository.findById(serviceId)).thenReturn(java.util.Optional.of(existingService));
        when(repository.save(existingService)).thenReturn(existingService);

        var response = service.cambiarEstado(serviceId, true);

        assertEquals(true, response.getEstado());
        verify(repository).save(existingService);
    }

    private Servicio service(UUID id, String name, boolean active) {
        return new Servicio(id, name, "Description", new BigDecimal("500005"), 27, active, null, null);
    }
}
