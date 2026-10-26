package com.lavarapido.backend_vehicular.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // Impide arrancar con una clave de firma demasiado corta.
    @PostConstruct
    void validateSigningKey() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT signing key must be at least 32 bytes");
        }
    }

    private SecretKey getKey() {
        // La misma clave configurada firma el token y verifica su firma despues.
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // El JWT identifica por UUID inmutable; el rol siempre se consulta en la BD.
    public String generateToken(UUID userId, String email) {

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                + 1000L * 60 * 60
                        )
                )
                .signWith(getKey())
                .compact();
    }

    // Recupera el UUID firmado que identifica la cuenta de forma inmutable.
    public UUID extractUserId(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    // Recupera el correo firmado para invalidar sesiones tras un cambio de correo.
    public String extractEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    // Verifica firma y caducidad; un JWT inválido no autentica.
    public boolean isTokenValid(String token) {
        try {
            // El parser verifica firma y expiracion antes de aceptar el contenido.
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        // Solo se leen claims de un JWT firmado con la clave configurada.
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
