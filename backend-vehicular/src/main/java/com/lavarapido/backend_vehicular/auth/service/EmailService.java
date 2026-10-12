package com.lavarapido.backend_vehicular.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String appFrontendUrl;
    private final String mailFrom;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.frontend.url}") String appFrontendUrl,
                        @Value("${app.mail.from:${spring.mail.username:}}") String mailFrom) {
        this.mailSender = mailSender;
        this.appFrontendUrl = appFrontendUrl;
        this.mailFrom = mailFrom;
    }

    /**
     * Envía el correo de recuperación de contraseña.
     * El enlace lleva el token en texto plano (no el hash).
     * El backend hashea al recibirlo para comparar.
     *
     * @param destinatario correo del usuario
     * @param tokenPlano   token generado antes de hashear
     */
    public void enviarCorreoRecuperacion(String destinatario, String tokenPlano) {

        // Arma el enlace que irá en el correo
        // ejemplo: http://localhost:5173/reset-password?token=abc123
        String frontendBase = appFrontendUrl.endsWith("/")
                ? appFrontendUrl.substring(0, appFrontendUrl.length() - 1)
                : appFrontendUrl;
        String enlace = frontendBase + "/reset-password?token=" + tokenPlano;

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(mailFrom);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Recuperación de contraseña – Lava Rápido Vehicular");
        mensaje.setText(
            "Hola,\n\n" +
            "Recibimos una solicitud para restablecer tu contraseña.\n\n" +
            "Haz clic en el siguiente enlace (válido por 30 minutos):\n" +
            enlace + "\n\n" +
            "Si usas la aplicacion movil, copia este token de un solo uso en la pantalla de recuperacion:\n" +
            tokenPlano + "\n\n" +
            "Si no solicitaste esto, ignora este correo.\n\n" +
            "– Equipo Lava Rápido Vehicular"
        );

        // Aquí se entrega el mensaje al servidor SMTP configurado.
        mailSender.send(mensaje);
    }
}
