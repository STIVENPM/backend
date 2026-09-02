package com.lavarapido.backend_vehicular.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String appFrontendUrl;

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
        String enlace = appFrontendUrl + "/reset-password?token=" + tokenPlano;

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("Recuperación de contraseña – Lava Rápido Vehicular");
        mensaje.setText(
            "Hola,\n\n" +
            "Recibimos una solicitud para restablecer tu contraseña.\n\n" +
            "Haz clic en el siguiente enlace (válido por 30 minutos):\n" +
            enlace + "\n\n" +
            "Si no solicitaste esto, ignora este correo.\n\n" +
            "– Equipo Lava Rápido Vehicular"
        );

        mailSender.send(mensaje);
    }
}
