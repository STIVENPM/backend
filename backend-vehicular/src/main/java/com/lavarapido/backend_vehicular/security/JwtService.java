package com.lavarapido.backend_vehicular.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    // se inyecta la clave secreta desde application.properties por seguridad
    @Value("${jwt.secret}")
    private String secret;

    // genera la clave de firma usando el algoritmo hmac sha a partir del secreto configurado
    private SecretKey getKey() {

        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    // crea un token jwt con una duracion de 1 hora para el usuario autenticado
    public String generateToken(String email) {

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())

                // tiempo de expiracion calculado en milisegundos (1 hora)
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                + 1000L * 60 * 60
                        )
                )

                .signWith(getKey())
                .compact();
    }

    // obtiene el identificador del usuario (subject) contenido en el cuerpo del token
    public String extractEmail(String token) {

        return getClaims(token).getSubject();
    }

    // verifica si el token es estructuralmente valido y no ha expirado
    public boolean isTokenValid(String token) {

        try {

            getClaims(token);

            return true;

        } catch (JwtException | IllegalArgumentException e) {

            // retorna falso si la firma es invalida o el formato es incorrecto
            return false;
        }
    }

    // procesa y valida la firma del token para extraer la carga util (claims)
    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}