package com.lavarapido.backend_vehicular.servicios.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lavarapido.backend_vehicular.servicios.dto.ServicioRequestDTO;
import com.lavarapido.backend_vehicular.servicios.dto.ServicioResponseDTO;
import com.lavarapido.backend_vehicular.servicios.service.ServicioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;

    @PostMapping
    public ResponseEntity<ServicioResponseDTO> crear(@Valid @RequestBody ServicioRequestDTO request) {
        ServicioResponseDTO creado = servicioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    public ResponseEntity<List<ServicioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(servicioService.listarTodos());
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<ServicioResponseDTO>> listarDisponibles() {
        return ResponseEntity.ok(servicioService.listarDisponibles());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ServicioResponseDTO>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(servicioService.buscarPorNombre(nombre));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioResponseDTO> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(servicioService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioResponseDTO> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ServicioRequestDTO request) {
        return ResponseEntity.ok(servicioService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ServicioResponseDTO> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam boolean activo) {
        return ResponseEntity.ok(servicioService.cambiarEstado(id, activo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        servicioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}