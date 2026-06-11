package com.lavarapido.backend_vehicular.shared.config;

import com.lavarapido.backend_vehicular.security.JwtAuthenticationFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http
    ) throws Exception {

        http

            // 🔓 Habilita CORS usando la configuracion definida en WebConfig
            // permite que frontend (React/Vite) y Swagger puedan consumir el backend
            .cors(Customizer.withDefaults())

            // 🔓 Se desactiva CSRF porque este proyecto usa JWT stateless
            // CSRF aplica principalmente cuando se usan sesiones y cookies
            .csrf(csrf -> csrf.disable())

            // 🔒 SIN SESIONES
            // cada request debe autenticarse con JWT
            // Spring no guardara sesiones en servidor
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // 🔓 Se desactiva el formulario de login automatico de Spring
            // evitamos que Spring muestre su login por defecto
            .formLogin(form -> form.disable())

            // 🔓 Se desactiva autenticacion basica tipo navegador
            // no usamos usuario/password por Basic Auth
            .httpBasic(basic -> basic.disable())

            .authorizeHttpRequests(auth -> auth

                // 🔓 RUTAS PUBLICAS
                // estas rutas no requieren JWT
                // login y registro deben quedar accesibles sin token
                .requestMatchers(
                    "/api/users/login",
                    "/api/users/register",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/error"
                ).permitAll()

                // 🔒 TODO LO DEMAS
                // cualquier otra ruta necesita token JWT valido
                .anyRequest().authenticated()
            )

            // 🔐 FILTRO JWT PERSONALIZADO
            // corre antes del filtro interno de Spring Security
            // valida token antes de permitir acceso a rutas privadas
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        // 🔐 BCrypt
        // usado para encriptar contrasenas en registro
        // y validar contrasena en login
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {

        // 🔕 Evita el warning:
        // Using generated security password
        // no usamos usuarios en memoria realmente
        // solo se declara para evitar el comportamiento por defecto
        return new InMemoryUserDetailsManager();
    }
}
