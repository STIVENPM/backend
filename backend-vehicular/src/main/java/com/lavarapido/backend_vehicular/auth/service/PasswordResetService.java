package com.lavarapido.backend_vehicular.auth.service;

import com.lavarapido.backend_vehicular.auth.entity.TokenRecuperacion;
import com.lavarapido.backend_vehicular.auth.exception.EmailDeliveryException;
import com.lavarapido.backend_vehicular.auth.exception.UserNotFoundException;
import com.lavarapido.backend_vehicular.auth.repository.TokenRecuperacionRepository;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.MailException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository           userRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final EmailService             emailService;
    private final PasswordEncoder          passwordEncoder;

    // Tiempo de vida del token en minutos
    private static final int TTL_MINUTOS = 30;

    // ── PASO 1: El usuario pide recuperar su contraseña ──────────────
    /**
     * Busca el usuario por email, genera un token seguro,
     * lo guarda hasheado en BD y envía el enlace por correo.
     *
    * Si el email no existe se informa al controlador mediante una
    * excepcion de dominio para devolver una respuesta clara.
     */
    @Transactional
    public void solicitarRecuperacion(String email, String ipSolicitante) {

        User usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("No existe un usuario con ese correo"));

        String tokenPlano = generarTokenSeguro();
        String tokenHash = hashearSHA256(tokenPlano);

        TokenRecuperacion token = new TokenRecuperacion();
        token.setUsuario(usuario);
        token.setTokenHash(tokenHash);
        token.setExpiracionAt(LocalDateTime.now().plusMinutes(TTL_MINUTOS));
        token.setUsado(false);
        token.setIpSolicitante(ipSolicitante);
        tokenRepository.save(token);

        try {
            emailService.enviarCorreoRecuperacion(email, tokenPlano);
        } catch (MailException | IllegalStateException exception) {
            throw new EmailDeliveryException("No fue posible enviar el correo de recuperacion", exception);
        }
    }

    // ── PASO 2: El usuario hace clic en el enlace y manda nueva contraseña ──
    /**
     * Valida el token recibido del frontend:
     *   1. Existe en BD (por hash)
     *   2. No ha sido usado
     *   3. No ha expirado
     * Si todo es válido, actualiza la contraseña y marca el token como usado.
     */
    @Transactional
    public void resetearContrasena(String tokenPlano, String nuevaContrasena) {

        // Hashea el token recibido para buscarlo en BD
        String tokenHash = hashearSHA256(tokenPlano);

        // Busca el token en BD
        TokenRecuperacion token = tokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new RuntimeException("Token inválido"));

        // Verifica que no haya sido usado antes
        if (token.getUsado()) {
            throw new RuntimeException("El token ya fue utilizado");
        }

        // Verifica que no haya expirado
        if (LocalDateTime.now().isAfter(token.getExpiracionAt())) {
            throw new RuntimeException("El token ha expirado");
        }

        // Actualiza la contraseña del usuario con BCrypt
        User usuario = token.getUsuario();
        usuario.setPassword(passwordEncoder.encode(nuevaContrasena));
        userRepository.save(usuario);

        // Marca el token como usado para que no pueda reutilizarse
        token.setUsado(true);
        tokenRepository.save(token);
    }

    // ── Utilidades privadas ───────────────────────────────────────────

    /**
     * Genera un token aleatorio seguro de 32 bytes en Base64 URL-safe.
     * SecureRandom es criptográficamente seguro, no usar Math.random().
     */
    private String generarTokenSeguro() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hashea un String con SHA-256 y retorna el resultado en hexadecimal.
     * El hash tiene exactamente 64 caracteres, coincide con CHAR(64) en BD.
     */
    private String hashearSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre existe en Java, esto nunca debería ocurrir
            throw new RuntimeException("Error al hashear el token", e);
        }
    }
}
