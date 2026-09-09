package com.lavarapido.backend_vehicular.operadores.controller;

import com.lavarapido.backend_vehicular.operadores.dto.OperadorEstadoRequestDTO;
import com.lavarapido.backend_vehicular.operadores.dto.OperadorRequestDTO;
import com.lavarapido.backend_vehicular.operadores.dto.OperadorResponseDTO;
import com.lavarapido.backend_vehicular.operadores.dto.OperadoresDesactivadosResponseDTO;
import com.lavarapido.backend_vehicular.operadores.service.OperadorService;
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
@RequestMapping("/api/operadores")
@RequiredArgsConstructor
public class OperadorController {

    private final OperadorService operadorService;

    @PostMapping
    public ResponseEntity<OperadorResponseDTO> crear(@Valid @RequestBody OperadorRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(operadorService.crear(request));
    }

    @GetMapping
    public ResponseEntity<List<OperadorResponseDTO>> listarTodos() {
        return ResponseEntity.ok(operadorService.listarTodos());
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<OperadorResponseDTO> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody OperadorEstadoRequestDTO request) {
        return ResponseEntity.ok(operadorService.cambiarEstado(id, request));
    }

    @PatchMapping("/desactivar-todos")
    public ResponseEntity<OperadoresDesactivadosResponseDTO> desactivarTodos() {
        return ResponseEntity.ok(operadorService.desactivarTodos());
    }
}
