package com.lavarapido.backend_vehicular.controllers;

import com.lavarapido.backend_vehicular.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // ── ENDPOINT 1: El usuario pide recuperar su contraseña ──────────
    /**
     * Recibe el email, genera el token y envía el correo.
     * Siempre responde 200 OK aunque el email no exista (seguridad).
     *
     * POST /api/auth/forgot-password
     * Body: { "email": "usuario@correo.com" }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        // Captura la IP del solicitante para auditoría
        String ip = httpRequest.getRemoteAddr();

        passwordResetService.solicitarRecuperacion(request.email(), ip);

        // Siempre responde igual para no revelar si el email existe
        return ResponseEntity.ok("Si el correo está registrado, recibirás un enlace.");
    }

    // ── ENDPOINT 2: El usuario manda el token y la nueva contraseña ──
    /**
     * Valida el token y actualiza la contraseña.
     *
     * POST /api/auth/reset-password
     * Body: { "token": "abc123...", "nuevaContrasena": "NuevaPass1" }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetearContrasena(
            request.token(),
            request.nuevaContrasena()
        );

        return ResponseEntity.ok("Contraseña actualizada correctamente.");
    }

    // ── DTOs (Records) ────────────────────────────────────────────────
    // Records de Java: clases simples de solo lectura para recibir el JSON

    /** Body del primer endpoint */
    record ForgotPasswordRequest(String email) {}

    /** Body del segundo endpoint */
    record ResetPasswordRequest(String token, String nuevaContrasena) {}
}