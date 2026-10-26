package com.lavarapido.backend_vehicular.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Applies a small in-memory guard to public authentication endpoints. */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String route = route(request);
        if (route == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = route + ":" + clientAddress(request);
        if (!allow(key)) {
            response.setHeader("Retry-After", Long.toString(WINDOW.toSeconds()));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Demasiados intentos");
            return;
        }

        filterChain.doFilter(request, response);
        if (isSuccessfulAuthentication(route, response)) {
            attempts.remove(key);
        }
    }

    private boolean allow(String key) {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> entry.getValue().startedAt.plus(WINDOW).isBefore(now));
        return attempts.compute(key, (ignored, current) -> {
            if (current == null || current.startedAt.plus(WINDOW).isBefore(now)) {
                return new AttemptWindow(now, 1);
            }
            return new AttemptWindow(current.startedAt, current.count + 1);
        }).count <= MAX_ATTEMPTS;
    }

    private boolean isSuccessfulAuthentication(String route, HttpServletResponse response) {
        if ("login".equals(route)) {
            return response.getStatus() < 400;
        }
        return false;
    }

    private String route(HttpServletRequest request) {
        return switch (request.getRequestURI()) {
            case "/api/users/login" -> "login";
            case "/api/auth/forgot-password" -> "forgot-password";
            case "/api/auth/reset-password" -> "reset-password";
            default -> null;
        };
    }

    private String clientAddress(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private record AttemptWindow(Instant startedAt, int count) { }
}
