package com.lavarapido.backend_vehicular.reservas.controller;

import com.lavarapido.backend_vehicular.reservas.dto.ReservaRequestDTO;
import com.lavarapido.backend_vehicular.reservas.dto.ReservaResponseDTO;
import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.reservas.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    public ReservaController(ReservaService reservaService) { this.reservaService = reservaService; }

    // Valida los datos de la reserva y devuelve 201.
    @PostMapping
    public ResponseEntity<ReservaResponseDTO> crear(@Valid @RequestBody ReservaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crear(request));
    }

    // Expone reservas al panel administrador.
    @GetMapping
    public ResponseEntity<List<ReservaResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(reservaService.obtenerTodas());
    }

    // Consulta una reserva protegida por UUID.
    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponseDTO> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    // Consulta las reservas de un usuario autorizado.
    @GetMapping("/usuario/{id}")
    public ResponseEntity<List<ReservaResponseDTO>> obtenerPorUsuario(@PathVariable UUID id) {
        return ResponseEntity.ok(reservaService.obtenerPorUsuario(id));
    }

    // Recibe la transición solicitada en el parámetro estado.
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReservaResponseDTO> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam EstadoReserva estado) {
        return ResponseEntity.ok(reservaService.cambiarEstado(id, estado));
    }

    // Pide cancelar la reserva sin borrarla.
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponseDTO> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(reservaService.cancelar(id));
    }
}
