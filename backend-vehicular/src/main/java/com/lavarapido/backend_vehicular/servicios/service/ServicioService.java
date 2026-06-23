package com.lavarapido.backend_vehicular.servicios.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lavarapido.backend_vehicular.servicios.dto.ServicioRequestDTO;
import com.lavarapido.backend_vehicular.servicios.dto.ServicioResponseDTO;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
import com.lavarapido.backend_vehicular.servicios.repository.ServicioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository servicioRepository;

    public ServicioResponseDTO crear(ServicioRequestDTO request) {
        if (servicioRepository.existsByNombre(request.getNombre())) {
            throw new IllegalArgumentException("Ya existe un servicio con ese nombre");
        }

        Servicio servicio = new Servicio();
        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setPrecio(request.getPrecio());
        servicio.setDuracionMinutos(request.getDuracionMinutos());
        servicio.setEstado(true);

        Servicio guardado = servicioRepository.save(servicio);
        return mapearAResponse(guardado);
    }

    public List<ServicioResponseDTO> listarTodos() {
        return servicioRepository.findAll()
                .stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    public List<ServicioResponseDTO> listarDisponibles() {
        return servicioRepository.findByEstadoTrue()
                .stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    public List<ServicioResponseDTO> buscarPorNombre(String nombre) {
        return servicioRepository.findByNombreContainingIgnoreCase(nombre)
                .stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    public ServicioResponseDTO obtenerPorId(UUID id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));
        return mapearAResponse(servicio);
    }

    public ServicioResponseDTO actualizar(UUID id, ServicioRequestDTO request) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));

        if (!servicio.getNombre().equalsIgnoreCase(request.getNombre())
                && servicioRepository.existsByNombre(request.getNombre())) {
            throw new IllegalArgumentException("Ya existe un servicio con ese nombre");
        }

        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setPrecio(request.getPrecio());
        servicio.setDuracionMinutos(request.getDuracionMinutos());

        Servicio actualizado = servicioRepository.save(servicio);
        return mapearAResponse(actualizado);
    }

    public ServicioResponseDTO cambiarEstado(UUID id, boolean nuevoEstado) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));

        servicio.setEstado(nuevoEstado);
        Servicio actualizado = servicioRepository.save(servicio);
        return mapearAResponse(actualizado);
    }

    public void eliminar(UUID id) {
        cambiarEstado(id, false);
    }

    private ServicioResponseDTO mapearAResponse(Servicio servicio) {
        return new ServicioResponseDTO(
                servicio.getIdServicio(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getPrecio(),
                servicio.getDuracionMinutos(),
                servicio.getEstado(),
                servicio.getCreatedAt(),
                servicio.getUpdatedAt()
        );
    }
}