package com.lavarapido.backend_vehicular.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private static final String SECRET = "jwt-test-secret-that-is-at-least-32-bytes";

    @Test
    void tokenIdentifiesImmutableUserIdAndCarriesCurrentEmail() {
        JwtService service = service();
        UUID userId = UUID.randomUUID();
        String token = service.generateToken(userId, "new@example.test");

        assertEquals(userId, service.extractUserId(token));
        assertEquals("new@example.test", service.extractEmail(token));
    }

    @Test
    void legacyEmailSubjectCannotBeParsedAsAnAccountId() {
        JwtService service = service();
        String token = Jwts.builder()
                .subject("old@example.test")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThrows(IllegalArgumentException.class, () -> service.extractUserId(token));
    }

    private JwtService service() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "jwtSecret", SECRET);
        return service;
    }
}
