package com.lavarapido.backend_vehicular.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(
                    CorsRegistry registry
            ) {

                registry.addMapping("/**")

                        // 🔓 ORÍGENES PERMITIDOS
                        // NO usar allowedOrigins("*") junto con allowCredentials(true)
                        // porque Spring lanza IllegalArgumentException
                        // Aquí declaramos explícitamente los orígenes válidos
                        .allowedOrigins(
                                "http://localhost:5173", // React / Frontend
                                "http://localhost:8081" // Swagger / Backend
                                
                        )

                        // 🔓 MÉTODOS HTTP PERMITIDOS
                        .allowedMethods(
                                "GET",
                                "POST",
                                "PUT",
                                "DELETE",
                                "OPTIONS"
                        )

                        // 🔓 HEADERS PERMITIDOS
                        .allowedHeaders(
                                "Authorization",
                                "Content-Type",
                                "Accept"
                        )

                        // 🔓 PERMITIR CREDENCIALES
                        // necesario para JWT / sesiones / auth headers
                        .allowCredentials(true);
            }
        };
    }
}