package com.lavarapido.backend_vehicular.auth.service;

import com.lavarapido.backend_vehicular.auth.exception.EmailDeliveryException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsBrevoJsonWithEncodedResetLinkAndMobileToken() throws IOException {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> accept = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/smtp/email", exchange -> {
            method.set(exchange.getRequestMethod());
            apiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            accept.set(exchange.getRequestHeaders().getFirst("Accept"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
        try {
            service(server).enviarCorreoRecuperacion("user@example.test", "token+con espacio/á<&");

            var json = objectMapper.readTree(body.get());
            assertEquals("POST", method.get());
            assertEquals("test-api-key", apiKey.get());
            assertEquals("application/json", contentType.get());
            assertEquals("application/json", accept.get());
            assertEquals("noreply@example.test", json.path("sender").path("email").asString());
            assertEquals("Lava Rápido Vehicular", json.path("sender").path("name").asString());
            assertEquals("user@example.test", json.path("to").get(0).path("email").asString());
            assertEquals("Recuperación de contraseña – Lava Rápido Vehicular", json.path("subject").asString());
            assertTrue(json.path("textContent").asString().contains(
                    "https://app.example.test/reset-password?token=token%2Bcon+espacio%2F%C3%A1%3C%26"));
            assertTrue(json.path("textContent").asString().contains("token+con espacio/á<&"));
            assertTrue(json.path("textContent").asString().contains("válido por 30 minutos"));
            String html = json.path("htmlContent").asString();
            assertTrue(html.contains("Restablece tu contraseña"));
            assertTrue(html.contains("href=\"https://app.example.test/reset-password?token=token%2Bcon+espacio%2F%C3%A1%3C%26\""));
            assertTrue(html.contains("token+con espacio/á&lt;&amp;"));
            assertTrue(html.contains("30 minutos"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void nonSuccessStatusIsDeliveryFailure() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/smtp/email", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });
        server.start();
        try {
            EmailDeliveryException exception = assertThrows(EmailDeliveryException.class,
                    () -> service(server).enviarCorreoRecuperacion("user@example.test", "token"));
            assertEquals("delivery", exception.failureCategory());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void connectionFailureIsDeliveryFailure() throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new ConnectException("connection failed"));
        EmailService service = new EmailService(
                client, URI.create("https://api.brevo.com/v3/smtp/email"), objectMapper,
                "https://app.example.test", "noreply@example.test", "test-api-key");

        EmailDeliveryException exception = assertThrows(EmailDeliveryException.class,
                () -> service.enviarCorreoRecuperacion("user@example.test", "token"));
        assertEquals("connection", exception.failureCategory());
    }

    private EmailService service(HttpServer server) {
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v3/smtp/email");
        return new EmailService(HttpClient.newHttpClient(), endpoint, objectMapper,
                "https://app.example.test/", "noreply@example.test", "test-api-key");
    }
}
