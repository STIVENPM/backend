package com.lavarapido.backend_vehicular.pagos.service;

import tools.jackson.databind.JsonNode;
import com.lavarapido.backend_vehicular.pagos.config.WompiProperties;
import com.lavarapido.backend_vehicular.pagos.config.WompiConfigurationService;
import com.lavarapido.backend_vehicular.pagos.exception.WompiConfiguracionException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WompiSignatureService {
    // Firma datos enviados al widget y verifica que un webhook recibido
    // coincida con el secreto de eventos configurado en este servidor.
    private static final Logger logger = LoggerFactory.getLogger(WompiSignatureService.class);
    private final WompiProperties properties;
    private final WompiConfigurationService configurationService;

    // Firma referencia, monto y moneda para abrir el Widget.
    public String crearFirmaIntegridad(String referencia, long montoEnCentavos, String moneda) {
        configurationService.validarWidget();
        // El orden de concatenacion importa: referencia, centavos, moneda y
        // secreto de integridad. La web recibe el hash, nunca el secreto.
        return sha256(referencia + montoEnCentavos + moneda + requiredSecret(properties.getIntegritySecret(), "integridad"));
    }

    // Compara el checksum recibido con la firma calculada.
    public boolean firmaWebhookValida(JsonNode evento, String checksumHeader) {
        configurationService.validarEventos();
        JsonNode signature = evento.path("signature");
        JsonNode propertyNames = signature.path("properties");
        // Wompi puede entregar el checksum en el header o en el cuerpo.
        // Si hay un header no vacio, este tiene prioridad para la comparacion.
        String recibido = checksumHeader != null && !checksumHeader.isBlank()
                ? checksumHeader : signature.path("checksum").asString();

        if (checksumHeader != null && !checksumHeader.isBlank()) {
            logger.info("Validando webhook Wompi usando checksum del header X-Event-Checksum");
        } else if (signature.hasNonNull("checksum") && !signature.path("checksum").asText().isBlank()) {
            logger.info("Validando webhook Wompi usando checksum del body signature.checksum");
        }

        // Sin lista de campos, checksum o timestamp no hay datos suficientes
        // para reconstruir la firma y el controlador rechazara el webhook.
        if (!propertyNames.isArray() || recibido == null || recibido.isBlank() || evento.path("timestamp").isMissingNode()) return false;

        StringBuilder contenido = new StringBuilder();
        // Se concatenan, en el orden indicado por signature.properties, los
        // valores de data; luego timestamp y secreto de eventos.
        for (JsonNode property : propertyNames) {
            String path = property.asString();
            if (path == null || path.isBlank()) return false;
            JsonNode value = leerRuta(evento.path("data"), path);
            String valorFirmado = valorEscalar(value);
            if (valorFirmado == null) return false;
            contenido.append(valorFirmado);
        }
        String timestamp = valorEscalar(evento.path("timestamp"));
        if (timestamp == null) return false;
        contenido.append(timestamp);
        contenido.append(requiredSecret(properties.getEventsSecret(), "eventos"));

        String recibidoNormalizado = recibido.toLowerCase(Locale.ROOT);
        // Compara los bytes de ambos hashes sin usar una igualdad de texto
        // que termine antes al encontrar el primer caracter distinto.
        return MessageDigest.isEqual(
                sha256(contenido.toString()).getBytes(StandardCharsets.UTF_8),
                recibidoNormalizado.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode leerRuta(JsonNode root, String path) {
        // Permite propiedades anidadas como transaction.id dentro de data.
        JsonNode current = root;
        for (String segment : path.split("\\.")) current = current.path(segment);
        return current;
    }

    private String valorEscalar(JsonNode value) {
        if (value == null || !value.isValueNode() || value.isNull()) return null;
        if (value.isString()) return value.asString();
        if (value.isNumber() || value.isBoolean()) return value.toString();
        return null;
    }

    private String requiredSecret(String value, String nombre) {
        // Una clave ausente es un error de configuracion, no un webhook invalido.
        if (value == null || value.isBlank()) {
            throw new WompiConfiguracionException("No está configurado el secreto de " + nombre + " de Wompi");
        }
        return value;
    }

    private String sha256(String value) {
        // SHA-256 produce el digest que se representa en hexadecimal.
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
