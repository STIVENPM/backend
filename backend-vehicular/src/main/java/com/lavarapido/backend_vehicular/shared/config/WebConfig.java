package com.lavarapido.backend_vehicular.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Configura que origenes web pueden llamar a la API desde un navegador.
 * El control de acceso por rol sigue ocurriendo en SecurityConfig. */
@Configuration
public class WebConfig {

    // Separa varios origenes con coma; en desarrollo, Vite usa localhost:5173.
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(
                    CorsRegistry registry
            ) {

                // Aplica la politica CORS a todas las rutas HTTP del backend.
                registry.addMapping("/**")

                        // 🔓 ORÍGENES PERMITIDOS
                        // NO usar allowedOrigins("*") junto con allowCredentials(true)
                        // porque Spring lanza IllegalArgumentException
                        // Aquí declaramos explícitamente los orígenes válidos
                        .allowedOrigins(Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim).filter(value -> !value.isEmpty()).toArray(String[]::new))

                        // 🔓 MÉTODOS HTTP PERMITIDOS
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")

                        // 🔓 HEADERS PERMITIDOS
                        .allowedHeaders(
                                // Authorization permite que el navegador envie el JWT Bearer.
                                "Authorization",
                                "Content-Type",
                                "Accept"
                        )

                        // 🔓 PERMITIR CREDENCIALES
                        // El JWT se envía en Authorization, sin cookies entre orígenes.
                        .allowCredentials(false);
            }
        };
    }
}
