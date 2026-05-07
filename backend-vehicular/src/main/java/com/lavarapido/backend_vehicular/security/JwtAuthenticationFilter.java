package com.lavarapido.backend_vehicular.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // 🔓 PERMITIR PREFLIGHT OPTIONS
        // navegador y Swagger envian OPTIONS antes de POST/PUT
        // si esto se bloquea aparece error 403 aunque la ruta sea publica
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {

            filterChain.doFilter(request, response);
            return;
        }

        // 🔍 LEER HEADER AUTHORIZATION
        // si no existe Authorization o no inicia con Bearer
        // significa que la ruta puede ser publica (login, registro, swagger)
        final String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null ||
            !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // ✂️ EXTRAER TOKEN JWT
        // se elimina el prefijo "Bearer "
        String token = authHeader.substring(7);

        // 🔐 VALIDAR TOKEN
        // si el token expiro, fue modificado o es invalido
        // se rechaza inmediatamente con 401
        if (!jwtService.isTokenValid(token)) {

            response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
            );

            response.getWriter().write("Token invalido");

            return;
        }

        // 🔥 EXTRAER EMAIL DEL TOKEN
        // el email identifica al usuario autenticado
        String email =
                jwtService.extractEmail(token);

        // 🔥 CREAR OBJETO DE AUTENTICACION
        // no usamos password aqui porque JWT ya fue validado
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.emptyList()
                );

        // 🔥 GUARDAR AUTENTICACION EN SPRING SECURITY
        // desde este punto Spring reconoce al usuario como autenticado
        SecurityContextHolder
                .getContext()
                .setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}