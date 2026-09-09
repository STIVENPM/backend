package com.lavarapido.backend_vehicular.asignaciones.controller;

import com.lavarapido.backend_vehicular.asignaciones.dto.AsignacionEstadoRequestDTO;
import com.lavarapido.backend_vehicular.asignaciones.dto.AsignacionRequestDTO;
import com.lavarapido.backend_vehicular.asignaciones.dto.AsignacionResponseDTO;
import com.lavarapido.backend_vehicular.asignaciones.service.AsignacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {

    private final AsignacionService asignacionService;

    @PostMapping
    public ResponseEntity<AsignacionResponseDTO> crear(@Valid @RequestBody AsignacionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignacionService.crear(request));
    }

    @GetMapping("/mis-asignaciones")
    public ResponseEntity<List<AsignacionResponseDTO>> obtenerMisAsignaciones() {
        return ResponseEntity.ok(asignacionService.obtenerMisAsignaciones());
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<AsignacionResponseDTO> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody AsignacionEstadoRequestDTO request) {
        return ResponseEntity.ok(asignacionService.cambiarEstado(id, request));
    }
}
