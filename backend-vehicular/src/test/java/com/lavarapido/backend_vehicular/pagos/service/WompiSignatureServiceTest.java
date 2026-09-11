package com.lavarapido.backend_vehicular.pagos.service;

import com.lavarapido.backend_vehicular.pagos.config.WompiProperties;
import com.lavarapido.backend_vehicular.pagos.exception.WompiConfiguracionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WompiSignatureServiceTest {

    private static final String SECRET = "super-secret";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WompiProperties properties;
    private WompiSignatureService service;

    @BeforeEach
    void setUp() {
        properties = new WompiProperties();
        properties.setEventsSecret(SECRET);
        service = new WompiSignatureService(properties);
    }

    @Test
    void firmaWebhookValida_aceptaChecksumEnMinusculasCuandoSeUsaBody() throws Exception {
        JsonNode evento = construirEvento("abc123", "approved", "1700");

        assertTrue(service.firmaWebhookValida(evento, null));
    }

    @Test
    void firmaWebhookValida_aceptaChecksumEnMayusculasCuandoSeUsaHeader() throws Exception {
        JsonNode evento = construirEvento("abc123", "approved", "1700");
        String checksumMayusculas = construirChecksum("abc123", "approved", "1700", SECRET).toUpperCase();

        assertTrue(service.firmaWebhookValida(evento, checksumMayusculas));
    }

    @Test
    void firmaWebhookValida_rechazaChecksumInvalido() throws Exception {
        JsonNode evento = construirEvento("abc123", "approved", "1700");

        assertFalse(service.firmaWebhookValida(evento, "checksum-invalido"));
    }

    @Test
    void firmaWebhookValida_lanzaConfiguracionExceptionCuandoFaltaSecret() throws Exception {
        properties.setEventsSecret("   ");
        service = new WompiSignatureService(properties);

        JsonNode evento = construirEvento("abc123", "approved", "1700");

        assertThrows(WompiConfiguracionException.class, () -> service.firmaWebhookValida(evento, null));
    }

    private JsonNode construirEvento(String id, String status, String timestamp) throws Exception {
        ObjectNode evento = objectMapper.createObjectNode();
        evento.put("timestamp", timestamp);

        ObjectNode signature = evento.putObject("signature");
        signature.putArray("properties").add("id").add("status");
        signature.put("checksum", construirChecksum(id, status, timestamp, SECRET));

        ObjectNode data = evento.putObject("data");
        data.put("id", id);
        data.put("status", status);

        return evento;
    }

    private String construirChecksum(String id, String status, String timestamp, String secret) {
        return sha256(id + status + timestamp + secret);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
