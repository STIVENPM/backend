package com.lavarapido.backend_vehicular.calificaciones.controller;
import com.lavarapido.backend_vehicular.calificaciones.dto.*;
import com.lavarapido.backend_vehicular.calificaciones.service.CalificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/calificaciones") @RequiredArgsConstructor
public class CalificacionController {
    private final CalificacionService service;
    @PostMapping public ResponseEntity<CalificacionResponseDTO> crear(@Valid @RequestBody CalificacionRequestDTO request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request)); }
    @GetMapping("/reserva/{idReserva}") public ResponseEntity<CalificacionResponseDTO> obtenerPorReserva(@PathVariable UUID idReserva) { return ResponseEntity.ok(service.obtenerPorReserva(idReserva)); }
}
