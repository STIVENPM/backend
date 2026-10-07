
package com.lavarapido.backend_vehicular.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import org.springframework.security.core.AuthenticationException;
import com.lavarapido.backend_vehicular.users.entity.User;

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
            filterChain.doFilter(request, response);
            return;
        }

        // Obtener el header Authorization
        final String authHeader = request.getHeader("Authorization");

        // Si no existe token, continuar como usuario anónimo
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el JWT
        String token = authHeader.substring(7);

        // Validar el JWT
        if (!jwtService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token invalido");
            return;
        }

        // Extraer información del JWT
        String email = jwtService.extractEmail(token);
        // The signed token identifies the account; its old role claim is never trusted.
        final User user;
        final String role;
        try {
            user = accountAccessService.activeUser(email);
            role = accountAccessService.currentRole(user);
        } catch (AuthenticationException exception) {
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

        // Crear la autenticación del usuario
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        email,
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
