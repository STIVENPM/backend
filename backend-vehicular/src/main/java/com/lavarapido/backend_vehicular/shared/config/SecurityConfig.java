
package com.lavarapido.backend_vehicular.shared.config;

import com.lavarapido.backend_vehicular.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

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
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;


    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http
    ) throws Exception {

        http
            // =========================================================
            // CORS
            // =========================================================
            .cors(Customizer.withDefaults())

            // =========================================================
            // CSRF
            // =========================================================
            .csrf(csrf -> csrf.disable())

            // =========================================================
            // SESIONES
            // JWT = STATELESS
            // =========================================================
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // =========================================================
            // DESHABILITAR FORM LOGIN
            // =========================================================
            .formLogin(form -> form.disable())

            // =========================================================
            // DESHABILITAR HTTP BASIC
            // =========================================================
            .httpBasic(basic -> basic.disable())

            // =========================================================
            // AUTORIZACIÓN
            // =========================================================
            .authorizeHttpRequests(auth -> auth

                // =====================================================
                // ENDPOINTS PÚBLICOS
                // =====================================================
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


                // =====================================================
                // MARCAS
                // =====================================================

                // Buscar marcas -> ADMIN
                .requestMatchers(
                    "/api/marcas/pendientes",
                    "/api/marcas/buscar"
                ).hasRole("ADMIN")


                // Crear marca -> ADMIN
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/marcas"
                ).hasRole("ADMIN")


                // Actualizar marca -> ADMIN
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/marcas/**"
                ).hasRole("ADMIN")


                // Cambiar estado de marca -> ADMIN
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/marcas/**"
                ).hasRole("ADMIN")


                // Listar todas las marcas -> ADMIN
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/marcas"
                ).hasRole("ADMIN")


                // Obtener una marca por UUID -> ADMIN
                //
                // IMPORTANTE:
                // Se restringe {id} a formato UUID para evitar que
                // /api/marcas/activas sea interpretado como {id}.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/marcas/{id:[0-9a-fA-F-]{36}}"
                ).hasRole("ADMIN")


                // Listar marcas activas -> cualquier usuario autenticado
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/marcas/activas"
                ).authenticated()


                // =====================================================
                // SERVICIOS
                // =====================================================

                // Crear servicio -> ADMIN
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/servicios"
                ).hasRole("ADMIN")


                // Actualizar servicio -> ADMIN
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/servicios/**"
                ).hasRole("ADMIN")


                // Cambiar estado de servicio -> ADMIN
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/servicios/**"
                ).hasRole("ADMIN")


                // Consultar servicios -> AUTENTICADO
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/servicios/**"
                ).authenticated()


                // =====================================================
                // VEHÍCULOS
                // =====================================================

                // Listar todos los vehículos -> ADMIN
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/vehiculos"
                ).hasRole("ADMIN")


                // Los demás endpoints de vehículos requieren
                // autenticación mediante JWT.
                .requestMatchers(
                    "/api/vehiculos/**"
                ).authenticated()


                // =====================================================
                // RESERVAS
                // =====================================================

                // Listar todas las reservas -> ADMIN
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/reservas"
                ).hasRole("ADMIN")


                // Cancelar reserva -> AUTENTICADO
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/reservas/*/cancelar"
                ).authenticated()


                // Otros PATCH de reservas -> ADMIN
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/reservas/**"
                ).hasRole("ADMIN")


                // Crear reserva -> AUTENTICADO
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/reservas"
                ).authenticated()


                // Obtener reserva por ID -> AUTENTICADO
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/reservas/{id}"
                ).authenticated()


                // Obtener reservas de un usuario -> AUTENTICADO
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/reservas/usuario/{id}"
                ).authenticated()


                // =====================================================
                // CUALQUIER OTRO ENDPOINT
                // =====================================================
                .anyRequest().authenticated()
            )


            // =========================================================
            // JWT FILTER
            // =========================================================
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            );


        return http.build();
    }


    // =============================================================
    // PASSWORD ENCODER
    // =============================================================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // =============================================================
    // USER DETAILS SERVICE
    // =============================================================
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}

