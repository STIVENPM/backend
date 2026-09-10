package com.lavarapido.backend_vehicular.auditoria.controller;
import com.lavarapido.backend_vehicular.auditoria.dto.AuditoriaResponseDTO;
import com.lavarapido.backend_vehicular.auditoria.enums.*;
import com.lavarapido.backend_vehicular.auditoria.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.UUID;
@RestController @RequestMapping("/api/auditoria") @RequiredArgsConstructor
public class AuditoriaController {
    private final AuditoriaService service;
    @GetMapping public ResponseEntity<Page<AuditoriaResponseDTO>> listar(@RequestParam(required = false) UUID usuarioId, @RequestParam(required = false) AccionAuditoria accion, @RequestParam(required = false) ModuloAuditoria modulo, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta, @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) { return ResponseEntity.ok(service.listar(usuarioId, accion, modulo, fechaDesde, fechaHasta, pageable)); }
}
