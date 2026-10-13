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

    // El JWT identifica por email y caduca una hora después; no fija el rol.
    public String generateToken(String email) {

        return Jwts.builder()
                // El subject permite al filtro buscar la cuenta por correo.
                .subject(email)
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

    // Recupera el email firmado que identifica la cuenta.
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
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
