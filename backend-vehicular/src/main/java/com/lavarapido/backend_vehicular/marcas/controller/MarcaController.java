package com.lavarapido.backend_vehicular.marcas.controller;


import com.lavarapido.backend_vehicular.marcas.dto.MarcaRequestDTO;
import com.lavarapido.backend_vehicular.marcas.dto.MarcaResponseDTO;
import com.lavarapido.backend_vehicular.marcas.service.MarcaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/marcas")
@RequiredArgsConstructor
public class MarcaController {

    private final MarcaService marcaService;

    // 🔒 Restringido a ADMIN — regla definida en SecurityConfig
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody MarcaRequestDTO dto) {
        try {
            MarcaResponseDTO creada = marcaService.crear(dto);
            return ResponseEntity.ok(creada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Catálogo aprobado (cliente + admin) ───────────────────────────
    @GetMapping("/activas")
    public ResponseEntity<List<MarcaResponseDTO>> listarActivas() {
        return ResponseEntity.ok(marcaService.listarActivas());
    }

    // 🔒 Restringido a ADMIN — regla definida en SecurityConfig
    @GetMapping
    public ResponseEntity<List<MarcaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(marcaService.listarTodas());
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<MarcaResponseDTO>> listarPendientes() {
        return ResponseEntity.ok(marcaService.listarPendientes());
    }

    // 🔒 Restringido a ADMIN — regla definida en SecurityConfig
    @GetMapping("/buscar")
    public ResponseEntity<List<MarcaResponseDTO>> buscar(@RequestParam String nombre) {
        return ResponseEntity.ok(marcaService.buscar(nombre));
    }

    // ── Obtener por id ───────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(marcaService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // 🔒 Restringido a ADMIN — regla definida en SecurityConfig
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody MarcaRequestDTO dto) {
        try {
            MarcaResponseDTO actualizada = marcaService.actualizar(id, dto);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔒 Restringido a ADMIN — regla definida en SecurityConfig
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam boolean activo) {
        try {
            MarcaResponseDTO actualizada = marcaService.cambiarEstado(id, activo);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
