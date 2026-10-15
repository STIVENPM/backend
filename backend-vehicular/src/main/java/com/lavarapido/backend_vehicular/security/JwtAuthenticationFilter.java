
package com.lavarapido.backend_vehicular.security;

import com.lavarapido.backend_vehicular.users.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccountAccessService accountAccessService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Ignorar solicitudes OPTIONS de CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            // El preflight se deja pasar sin exigir token.
            filterChain.doFilter(request, response);
            return;
        }

        // Obtener el header Authorization
        final String authHeader = request.getHeader("Authorization");

        // Si no existe token, continuar como usuario anónimo
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // La regla de SecurityConfig decide despues si esta ruta admite anonimos.
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el JWT
        String token = authHeader.substring(7);

        // Validar el JWT
        if (!jwtService.isTokenValid(token)) {
            // Una firma o caducidad invalida corta la peticion antes del controlador.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token invalido");
            return;
        }

        // El UUID identifica la cuenta; el email firmado invalida sesiones tras cambiarlo.
        final User user;
        final String role;
        try {
            UUID userId = jwtService.extractUserId(token);
            String email = jwtService.extractEmail(token);
            user = accountAccessService.activeUser(userId);
            if (email == null || !email.equals(user.getEmail())) {
                throw new BadCredentialsException("Invalid session");
            }
            // El rol se consulta en BD en cada peticion; no se toma del JWT.
            role = accountAccessService.currentRole(user);
        } catch (AuthenticationException | IllegalArgumentException exception) {
            // Cuenta desactivada o rol invalido: se borra el contexto y se responde 401.
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Sesion invalida");
            return;
        }

        // Crear la autoridad que espera Spring Security.
        // hasRole("ADMIN") internamente busca ROLE_ADMIN.
        List<SimpleGrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + role
                        )
                );

        // El principal sigue siendo el correo para no cambiar los contratos internos.
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        authorities
                );

        // Registrar el usuario autenticado
        SecurityContextHolder
                .getContext()
                .setAuthentication(auth);

        // Continuar con la petición
        filterChain.doFilter(request, response);
    }
}
