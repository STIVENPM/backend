package com.lavarapido.backend_vehicular.notificaciones.controller;

import com.lavarapido.backend_vehicular.notificaciones.dto.NotificacionDTO;
import com.lavarapido.backend_vehicular.notificaciones.dto.PaginaNotificacionesDTO;
import com.lavarapido.backend_vehicular.notificaciones.service.NotificacionService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {
    private final NotificacionService service;
    public NotificacionController(NotificacionService service) { this.service = service; }

    @GetMapping
    public PaginaNotificacionesDTO listar(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas) {
        return PaginaNotificacionesDTO.from(service.listar(pagina, tamano, soloNoLeidas));
    }

    @GetMapping("/no-leidas/count")
    public Map<String, Long> noLeidas() { return Map.of("count", service.noLeidas()); }

    @PatchMapping("/{id}/leida")
    public NotificacionDTO marcarLeida(@PathVariable UUID id) { return service.marcarLeida(id); }

    @PatchMapping("/leidas")
    public Map<String, Integer> marcarTodas() { return Map.of("actualizadas", service.marcarTodas()); }

    public record DispositivoRequest(String instalacionId, String token) { }
    @PutMapping("/dispositivos")
    public ResponseEntity<Void> registrar(@RequestBody DispositivoRequest request) {
        service.registrarDispositivo(request.instalacionId(), request.token());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/dispositivos/{instalacionId}")
    public ResponseEntity<Void> desactivar(@PathVariable String instalacionId) {
        service.desactivarDispositivo(instalacionId);
        return ResponseEntity.noContent().build();
    }
}
