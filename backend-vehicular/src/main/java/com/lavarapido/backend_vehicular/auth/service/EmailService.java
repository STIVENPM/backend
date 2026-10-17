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
                "Si usas la aplicación móvil, copia este código de un solo uso en la pantalla de recuperación:\n" +
                tokenPlano + "\n\n" +
                "Si no solicitaste esto, ignora este correo.\n\n" +
                "– Equipo Lava Rápido Vehicular";

        String html = """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Restablecer contraseña</title>
                </head>
                <body style="margin:0;padding:0;background:#f2f5fa;font-family:Arial,Helvetica,sans-serif;color:#172443;">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0;">Tu enlace para restablecer la contraseña vence en 30 minutos.</div>
                  <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="background:#f2f5fa;padding:32px 12px;">
                    <tr><td align="center">
                      <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width:560px;background:#ffffff;border:1px solid #dce4f0;border-radius:16px;overflow:hidden;">
                        <tr><td style="padding:28px 32px;background:#163d86;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:.2px;">Lava Rápido Vehicular</td></tr>
                        <tr><td style="padding:36px 32px 28px;">
                          <p style="margin:0 0 8px;color:#2765bb;font-size:12px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;">Seguridad de tu cuenta</p>
                          <h1 style="margin:0 0 20px;color:#172443;font-size:27px;line-height:1.25;">Restablece tu contraseña</h1>
                          <p style="margin:0 0 16px;color:#43516a;font-size:16px;line-height:1.6;">Hola,</p>
                          <p style="margin:0 0 25px;color:#43516a;font-size:16px;line-height:1.6;">Recibimos una solicitud para restablecer tu contraseña. Usa este botón para crear una nueva:</p>
                          <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr><td style="border-radius:9px;background:#1768c1;">
                            <a href="%s" style="display:inline-block;padding:15px 25px;border-radius:9px;color:#ffffff;font-size:16px;font-weight:700;text-decoration:none;">Restablecer contraseña</a>
                          </td></tr></table>
                          <p style="margin:22px 0 0;color:#5b6880;font-size:14px;line-height:1.6;">Este enlace vence en <strong>30 minutos</strong>. Si el botón no funciona, copia y pega esta dirección en tu navegador:</p>
                          <p style="margin:8px 0 0;color:#1768c1;font-size:13px;line-height:1.6;word-break:break-all;"><a href="%s" style="color:#1768c1;text-decoration:underline;">%s</a></p>
                          <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="margin-top:28px;background:#f4f7fc;border:1px solid #dce4f0;border-radius:10px;"><tr><td style="padding:18px 20px;">
                            <p style="margin:0 0 8px;color:#172443;font-size:15px;font-weight:700;">¿Usas la aplicación móvil?</p>
                            <p style="margin:0 0 10px;color:#43516a;font-size:14px;line-height:1.5;">Copia este código de un solo uso en la pantalla de recuperación:</p>
                            <p style="margin:0;color:#163d86;font-family:Consolas,Monaco,monospace;font-size:15px;font-weight:700;word-break:break-all;">%s</p>
                          </td></tr></table>
                          <p style="margin:28px 0 0;color:#5b6880;font-size:14px;line-height:1.6;">Si no solicitaste este cambio, puedes ignorar este correo. Tu contraseña seguirá siendo la misma.</p>
                        </td></tr>
                        <tr><td style="padding:20px 32px;border-top:1px solid #e7ecf4;color:#738098;font-size:12px;line-height:1.5;">Equipo Lava Rápido Vehicular<br>Este mensaje se envió para ayudarte a proteger tu cuenta.</td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escapeHtml(enlace), escapeHtml(enlace), escapeHtml(enlace), escapeHtml(tokenPlano));

        // Brevo recibe remitente, destinatario y mensaje; la clave API va en el encabezado.
        String json = objectMapper.writeValueAsString(Map.of(
                "sender", Map.of("email", mailFrom, "name", "Lava Rápido Vehicular"),
                "to", List.of(Map.of("email", destinatario)),
                "subject", "Recuperación de contraseña – Lava Rápido Vehicular",
                "textContent", texto,
                "htmlContent", html));

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

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
