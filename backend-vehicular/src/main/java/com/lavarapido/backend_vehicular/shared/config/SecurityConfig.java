
package com.lavarapido.backend_vehicular.shared.config;

import com.lavarapido.backend_vehicular.security.JwtAuthenticationFilter;
import com.lavarapido.backend_vehicular.security.AuthRateLimitFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

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
    private final AuthRateLimitFilter authRateLimitFilter;
    private final Environment environment;


    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http
    ) throws Exception {

        http
            // Las rutas protegidas usan JWT; el servidor no mantiene una sesion de login.
            // =========================================================
            // CORS
            // =========================================================
            // Usa los origenes y headers permitidos que define WebConfig.
            .cors(Customizer.withDefaults())

            // =========================================================
            // CSRF
            // =========================================================
            // La API usa JWT en Authorization y no una sesion de navegador con cookie.
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
            // Estas respuestas son de la cadena de seguridad, antes del controlador.
            .exceptionHandling(exceptions -> exceptions
                // 401 = falta autenticacion; 403 = identidad autenticada sin permiso.
                .authenticationEntryPoint((request, response, exception) ->
                    response.sendError(401, "Sesion invalida"))
                .accessDeniedHandler((request, response, exception) ->
                    response.sendError(403, "Acceso denegado")))

            // =========================================================
            // AUTORIZACIÓN
            // =========================================================
            .authorizeHttpRequests(auth -> auth
                // Las reglas se evalúan en orden: las rutas especificas van primero.

                // =====================================================
                // ENDPOINTS PÚBLICOS
                // =====================================================
                // Registro, login y recuperación deben poder llamarse antes de tener un JWT.
                // permitAll omite la exigencia de autenticacion para estas rutas.
                .requestMatchers(
                    "/api/users/login",
                    "/api/users/register",

                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",

                    "/error"
                ).permitAll()

                // Keep API documentation public for local development only.
                // The production profile disables springdoc and must not publish these routes.
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs"
                ).access((authentication, context) ->
                    new org.springframework.security.authorization.AuthorizationDecision(
                        !environment.acceptsProfiles(Profiles.of("prod"))))
                .requestMatchers(HttpMethod.GET, "/healthz").permitAll()
                // Salud publica para diagnosticar el backend sin iniciar sesion.
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()

                // Wompi does not send JWT; integrity is checked in PagoController.
                // El webhook queda publico en esta capa para permitir la entrega externa.
                .requestMatchers(HttpMethod.POST, "/api/pagos/webhook").permitAll()


                // =====================================================
                // MARCAS
                // =====================================================

                // Buscar marcas -> ADMIN
                // hasRole("ADMIN") exige ROLE_ADMIN, creada desde el rol vigente en BD.
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


                // Voluntary cancellation is restricted to customers.
                // Ownership and business rules are enforced again in ReservaService.
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/reservas/*/cancelar"
                ).hasRole("USER")


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
                // OPERADORES
                // =====================================================
                // La gestion de operadores queda reservada a administradores.
                .requestMatchers(
                    "/api/operadores",
                    "/api/operadores/**"
                ).hasRole("ADMIN")

                // =====================================================
                // AUDITORIA Y LOGS DE ERRORES
                // =====================================================
                .requestMatchers(
                    "/api/auditoria/**",
                    "/api/log-errores/**"
                ).hasRole("ADMIN")

                // Calificaciones: el servicio valida propietario o ADMIN
                // cuando se consulta una reserva concreta.
                // La autenticacion aqui no reemplaza la comprobacion de propiedad en servicio.
                .requestMatchers(
                    "/api/calificaciones/**"
                ).authenticated()


                // =====================================================
                // ASIGNACIONES
                // =====================================================
                // ADMIN crea asignaciones; OPERATOR consulta y actualiza las suyas.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/asignaciones"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/asignaciones/mis-asignaciones"
                ).hasRole("OPERATOR")

                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/asignaciones/**"
                ).hasRole("OPERATOR")

                // La reconciliacion usa la llave privada y solo puede ejecutarla ADMIN.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/pagos/reserva/*/reconciliar"
                ).hasRole("ADMIN")


                // =====================================================
                // CUALQUIER OTRO ENDPOINT
                // =====================================================
                // Cualquier ruta no enumerada aun necesita una identidad autenticada.
               .anyRequest().authenticated() 
            )


            // =========================================================
            // JWT FILTER
            // =========================================================
            .addFilterBefore(
                authRateLimitFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .addFilterBefore(
                // Valida el Bearer antes de que Spring decida los permisos de la ruta.
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            );


        return http.build();
    }

    @Bean
    public FilterRegistrationBean<AuthRateLimitFilter> authRateLimitFilterRegistration(
            AuthRateLimitFilter filter
    ) {
        FilterRegistrationBean<AuthRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }


    // =============================================================
    // PASSWORD ENCODER
    // =============================================================
    @Bean
    public PasswordEncoder passwordEncoder() {
        // UserService usa este encoder al guardar y comprobar contrasenas.
        return new BCryptPasswordEncoder();
    }


    // =============================================================
    // USER DETAILS SERVICE
    // =============================================================
    @Bean
    public UserDetailsService userDetailsService() {
        // El login propio consulta users en BD; este almacen en memoria inicia vacio.
        return new InMemoryUserDetailsManager();
    }
}

