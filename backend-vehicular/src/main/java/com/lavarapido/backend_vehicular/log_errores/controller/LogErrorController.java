package com.lavarapido.backend_vehicular.log_errores.controller;
import com.lavarapido.backend_vehicular.log_errores.dto.LogErrorResponseDTO;
import com.lavarapido.backend_vehicular.log_errores.service.LogErrorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/log-errores") @RequiredArgsConstructor
public class LogErrorController {
    private final LogErrorService service;
    @GetMapping public ResponseEntity<Page<LogErrorResponseDTO>> listar(@RequestParam(required = false) Boolean resuelto, @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) { return ResponseEntity.ok(service.listar(resuelto, pageable)); }
    @PatchMapping("/{id}/resolver") public ResponseEntity<LogErrorResponseDTO> resolver(@PathVariable UUID id) { return ResponseEntity.ok(service.resolver(id)); }
}
