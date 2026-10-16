package com.lavarapido.backend_vehicular.vehiculos.controller;

import com.lavarapido.backend_vehicular.vehiculos.dto.VehiculoRequestDTO;
import com.lavarapido.backend_vehicular.vehiculos.dto.VehiculoResponseDTO;
import com.lavarapido.backend_vehicular.vehiculos.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;
    public VehiculoController(VehiculoService vehiculoService) { this.vehiculoService = vehiculoService; }

    // ── Crear vehículo (app móvil — cliente autenticado) ────────────
    // Valida el JSON y delega el alta del vehículo.
    @PostMapping
    public ResponseEntity<VehiculoResponseDTO> crear(@Valid @RequestBody VehiculoRequestDTO dto) {
        return ResponseEntity.ok(vehiculoService.crear(dto));
    }

    // ── Mis vehículos (app móvil — cliente autenticado) ─────────────
    // Consulta solo los vehículos del usuario actual.
    @GetMapping("/mis-vehiculos")
    public ResponseEntity<List<VehiculoResponseDTO>> listarMisVehiculos() {
        return ResponseEntity.ok(vehiculoService.listarMisVehiculos());
    }

    // ── Listar todos (web admin) ─────────────────────────────────────
    // Entrega todos los vehículos al panel.
    @GetMapping
    public ResponseEntity<List<VehiculoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(vehiculoService.listarTodos());
    }

    // ── Obtener por id ───────────────────────────────────────────────
    // Busca un vehículo por UUID.
    @GetMapping("/{id}")
    public ResponseEntity<VehiculoResponseDTO> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(vehiculoService.obtenerPorId(id));
    }

    // ── Actualizar ─────────────────────────────────────────────────
    // Recibe y valida los nuevos datos del vehículo.
    @PutMapping("/{id}")
    public ResponseEntity<VehiculoResponseDTO> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody VehiculoRequestDTO dto) {
        return ResponseEntity.ok(vehiculoService.actualizar(id, dto));
    }

    // ── Cambiar estado (activar / desactivar — borrado lógico) ──────
    // Recibe el cambio de estado sin borrar la fila.
    @PatchMapping("/{id}/estado")
    public ResponseEntity<VehiculoResponseDTO> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam boolean activo) {
        return ResponseEntity.ok(vehiculoService.cambiarEstado(id, activo));
    }
}
