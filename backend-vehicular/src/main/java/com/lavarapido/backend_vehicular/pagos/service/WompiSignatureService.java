package com.lavarapido.backend_vehicular.pagos.service;

import tools.jackson.databind.JsonNode;
import com.lavarapido.backend_vehicular.pagos.config.WompiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class WompiSignatureService {
    private final WompiProperties properties;

    public String crearFirmaIntegridad(String referencia, long montoEnCentavos, String moneda) {
        return sha256(referencia + montoEnCentavos + moneda + requiredSecret(properties.getIntegritySecret(), "integridad"));
    }

    public boolean firmaWebhookValida(JsonNode evento, String checksumHeader) {
        JsonNode signature = evento.path("signature");
        JsonNode propertyNames = signature.path("properties");
        String recibido = checksumHeader != null && !checksumHeader.isBlank()
                ? checksumHeader : signature.path("checksum").asString();
        if (!propertyNames.isArray() || recibido == null || recibido.isBlank() || evento.path("timestamp").isMissingNode()) return false;

        StringBuilder contenido = new StringBuilder();
        for (JsonNode property : propertyNames) {
            JsonNode value = leerRuta(evento.path("data"), property.asString());
            if (value.isMissingNode()) return false;
            contenido.append(value.isValueNode() ? value.asString() : value.toString());
        }
        contenido.append(evento.path("timestamp").asString());
        contenido.append(requiredSecret(properties.getEventsSecret(), "eventos"));
        return MessageDigest.isEqual(
                sha256(contenido.toString()).getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode leerRuta(JsonNode root, String path) {
        JsonNode current = root;
        for (String segment : path.split("\\.")) current = current.path(segment);
        return current;
    }

    private String requiredSecret(String value, String nombre) {
        if (value == null || value.isBlank()) throw new IllegalStateException("No está configurado el secreto de " + nombre + " de Wompi");
        return value;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
