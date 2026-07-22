package com.lavarapido.backend_vehicular.vehiculos.controller;

import com.lavarapido.backend_vehicular.vehiculos.dto.VehiculoRequestDTO;
import com.lavarapido.backend_vehicular.vehiculos.dto.VehiculoResponseDTO;
import com.lavarapido.backend_vehicular.vehiculos.service.VehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService vehiculoService;

    // ── Crear vehículo (app móvil — cliente autenticado) ────────────
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody VehiculoRequestDTO dto) {
        try {
            VehiculoResponseDTO creado = vehiculoService.crear(dto);
            return ResponseEntity.ok(creado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Mis vehículos (app móvil — cliente autenticado) ─────────────
    @GetMapping("/mis-vehiculos")
    public ResponseEntity<List<VehiculoResponseDTO>> listarMisVehiculos() {
        return ResponseEntity.ok(vehiculoService.listarMisVehiculos());
    }

    // ── Listar todos (web admin) ─────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<VehiculoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(vehiculoService.listarTodos());
    }

    // ── Obtener por id ───────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(vehiculoService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // ── Actualizar ─────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody VehiculoRequestDTO dto) {
        try {
            VehiculoResponseDTO actualizado = vehiculoService.actualizar(id, dto);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Cambiar estado (activar / desactivar — borrado lógico) ──────
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam boolean activo) {
        try {
            VehiculoResponseDTO actualizado = vehiculoService.cambiarEstado(id, activo);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
