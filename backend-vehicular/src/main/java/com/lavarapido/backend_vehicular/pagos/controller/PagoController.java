package com.lavarapido.backend_vehicular.pagos.controller;

import tools.jackson.databind.JsonNode;
import com.lavarapido.backend_vehicular.pagos.dto.PagoResponseDTO;
import com.lavarapido.backend_vehicular.pagos.dto.PagoWidgetResponseDTO;
import com.lavarapido.backend_vehicular.pagos.service.PagoService;
import com.lavarapido.backend_vehicular.pagos.service.WompiSignatureService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Slf4j
public class PagoController {
    private final PagoService pagoService;
    private final WompiSignatureService signatureService;

    @PostMapping("/reserva/{idReserva}")
    public ResponseEntity<PagoWidgetResponseDTO> iniciar(@PathVariable UUID idReserva) {
        PagoWidgetResponseDTO response = pagoService.iniciar(idReserva);
        return ResponseEntity.status(response.reutilizado() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    @GetMapping("/reserva/{idReserva}")
    public ResponseEntity<PagoResponseDTO> obtenerPorReserva(@PathVariable UUID idReserva) {
        return ResponseEntity.ok(pagoService.obtenerPorReserva(idReserva));
    }

    @PostMapping("/reserva/{idReserva}/reconciliar")
    public ResponseEntity<PagoResponseDTO> reconciliar(@PathVariable UUID idReserva) {
        return ResponseEntity.ok(pagoService.reconciliar(idReserva));
    }

    @PostMapping("/reserva/{idReserva}/verificar")
    public ResponseEntity<PagoResponseDTO> verificar(@PathVariable UUID idReserva,
                                                      @RequestBody VerificarTransaccionRequest request) {
        return ResponseEntity.ok(pagoService.verificar(idReserva, request.referencia(), request.transactionId()));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody JsonNode evento,
                                        @RequestHeader(value = "X-Event-Checksum", required = false) String checksum) {
        if (!signatureService.firmaWebhookValida(evento, checksum)) {
            log.warn("Webhook Wompi rechazado: firma inválida");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Webhook Wompi con firma válida recibido: evento={}", evento.path("event").asString());
        PagoService.ResultadoEvento resultado = pagoService.procesarEvento(evento);
        log.info("Webhook Wompi atendido: resultado={}", resultado);
        return ResponseEntity.ok().build();
    }

    public record VerificarTransaccionRequest(String referencia, String transactionId) { }
}
