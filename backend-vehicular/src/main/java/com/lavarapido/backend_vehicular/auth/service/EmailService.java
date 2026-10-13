package com.lavarapido.backend_vehicular.auth.service;

import com.lavarapido.backend_vehicular.auth.exception.EmailDeliveryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final URI BREVO_ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final URI endpoint;
    private final ObjectMapper objectMapper;
    private final String appFrontendUrl;
    private final String mailFrom;
    private final String apiKey;

    @Autowired
    public EmailService(ObjectMapper objectMapper,
                        @Value("${app.frontend.url}") String appFrontendUrl,
                        @Value("${app.mail.from:${spring.mail.username:}}") String mailFrom,
                        @Value("${BREVO_API_KEY}") String apiKey) {
        this(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(), BREVO_ENDPOINT,
                objectMapper, appFrontendUrl, mailFrom, apiKey);
    }

    // Permite verificar la petición con un servidor HTTP local, sin contactar a Brevo.
    EmailService(HttpClient httpClient, URI endpoint, ObjectMapper objectMapper,
                 String appFrontendUrl, String mailFrom, String apiKey) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.objectMapper = objectMapper;
        this.appFrontendUrl = appFrontendUrl;
        this.mailFrom = mailFrom;
        this.apiKey = apiKey;
    }

    public void enviarCorreoRecuperacion(String destinatario, String tokenPlano) {
        // El enlace apunta al frontend; el token se codifica para viajar en la URL.
        String frontendBase = appFrontendUrl.endsWith("/")
                ? appFrontendUrl.substring(0, appFrontendUrl.length() - 1)
                : appFrontendUrl;
        String enlace = frontendBase + "/reset-password?token="
                + URLEncoder.encode(tokenPlano, StandardCharsets.UTF_8);

        String texto = "Hola,\n\n" +
                "Recibimos una solicitud para restablecer tu contraseña.\n\n" +
                "Haz clic en el siguiente enlace (válido por 30 minutos):\n" +
                enlace + "\n\n" +
                "Si usas la aplicacion movil, copia este token de un solo uso en la pantalla de recuperacion:\n" +
                tokenPlano + "\n\n" +
                "Si no solicitaste esto, ignora este correo.\n\n" +
                "– Equipo Lava Rápido Vehicular";

        // Brevo recibe remitente, destinatario y mensaje; la clave API va en el encabezado.
        String json = objectMapper.writeValueAsString(Map.of(
                "sender", Map.of("email", mailFrom),
                "to", List.of(Map.of("email", destinatario)),
                "subject", "Recuperación de contraseña – Lava Rápido Vehicular",
                "textContent", texto));

        if (apiKey == null || apiKey.isBlank()) {
            throw new EmailDeliveryException("No fue posible enviar el correo de recuperacion",
                    new IllegalStateException("Brevo API key is missing"));
        }

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(RESPONSE_TIMEOUT)
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            // Un estado distinto de 2xx significa que el proveedor no aceptó el envío.
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new EmailDeliveryException("No fue posible enviar el correo de recuperacion",
                        new IOException("Brevo HTTP status " + response.statusCode()));
            }
        } catch (IOException exception) {
            throw new EmailDeliveryException("No fue posible enviar el correo de recuperacion", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EmailDeliveryException("No fue posible enviar el correo de recuperacion", exception);
        }
    }
}
