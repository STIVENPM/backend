package com.lavarapido.backend_vehicular.auth.controller;

import com.lavarapido.backend_vehicular.auth.service.PasswordResetService;
import com.lavarapido.backend_vehicular.auth.exception.EmailDeliveryException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

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
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        // Captura la IP del solicitante para auditoría
        String ip = httpRequest.getRemoteAddr();

        try {
            passwordResetService.solicitarRecuperacion(request.email(), ip);
        } catch (EmailDeliveryException exception) {
            // Registra el fallo SMTP sin revelar si la cuenta existe.
            logger.warn("No fue posible enviar el correo de recuperacion ({})", exception.getClass().getSimpleName());
        }

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
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetearContrasena(
            request.token(),
            request.nuevaContrasena()
        );

        return ResponseEntity.ok("Contraseña actualizada correctamente.");
    }

    // ── DTOs (Records) ────────────────────────────────────────────────
    // Records de Java: clases simples de solo lectura para recibir el JSON

    /** Body del primer endpoint */
    record ForgotPasswordRequest(
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no tiene un formato valido")
            String email) {}

    /** Body del segundo endpoint */
    record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72) String nuevaContrasena) {}
}
