# CONTEXTO BACKEND JAVA

## Índice

- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 
- 

## 

```java
package com.lavarapido.backend_vehicular.auth.controller;

import com.lavarapido.backend_vehicular.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // â”€â”€ ENDPOINT 1: El usuario pide recuperar su contraseÃ±a â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    /**
     * Recibe el email, genera el token y envÃ­a el correo.
     * Siempre responde 200 OK aunque el email no exista (seguridad).
     *
     * POST /api/auth/forgot-password
     * Body: { "email": "usuario@correo.com" }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        // Captura la IP del solicitante para auditorÃ­a
        String ip = httpRequest.getRemoteAddr();

        passwordResetService.solicitarRecuperacion(request.email(), ip);

        // Siempre responde igual para no revelar si el email existe
        return ResponseEntity.ok("Si el correo estÃ¡ registrado, recibirÃ¡s un enlace.");
    }

    // â”€â”€ ENDPOINT 2: El usuario manda el token y la nueva contraseÃ±a â”€â”€
    /**
     * Valida el token y actualiza la contraseÃ±a.
     *
     * POST /api/auth/reset-password
     * Body: { "token": "abc123...", "nuevaContrasena": "NuevaPass1" }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetearContrasena(
            request.token(),
            request.nuevaContrasena()
        );

        return ResponseEntity.ok("ContraseÃ±a actualizada correctamente.");
    }

    // â”€â”€ DTOs (Records) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Records de Java: clases simples de solo lectura para recibir el JSON

    /** Body del primer endpoint */
    record ForgotPasswordRequest(String email) {}

    /** Body del segundo endpoint */
    record ResetPasswordRequest(String token, String nuevaContrasena) {}
}
```

## 

```java
package com.lavarapido.backend_vehicular.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {

    private String email;
    private String password;
}
```

## 

```java
package com.lavarapido.backend_vehicular.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;
    private UserInfoDTO user;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDTO {

        private String userId;
        private String firstName;
        private String email;
        private String role;
    }
}
```

## 

```java
package com.lavarapido.backend_vehicular.auth.entity;

import com.lavarapido.backend_vehicular.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tokens_recuperacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRecuperacion {

    // Mapea id_token UUID PRIMARY KEY
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_token", updatable = false, nullable = false)
    private UUID idToken;

    // FK hacia users(user_id) â€” relaciÃ³n ManyToOne
    // Un usuario puede tener varios tokens histÃ³ricos
    @ManyToOne
    @JoinColumn(name = "fk_id_usuario", nullable = false)
    private User usuario;

    // Hash SHA-256 del token enviado al correo
    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    // Fecha y hora en que expira el token (backend define el TTL)
    @Column(name = "expiracion_at", nullable = false)
    private LocalDateTime expiracionAt;

    // true = ya fue usado para resetear, no puede usarse de nuevo
    @Column(name = "usado", nullable = false)
    private Boolean usado = false;

    // IP opcional de quien hizo la solicitud (auditorÃ­a)
    @Column(name = "ip_solicitante", length = 45)
    private String ipSolicitante;

    // Lo pone la BD automÃ¡ticamente, Java solo lo lee
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

## 

```java
package com.lavarapido.backend_vehicular.auth.repository;

import com.lavarapido.backend_vehicular.auth.entity.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, UUID> {

    // Busca un token por su hash para validarlo cuando el usuario hace clic en el enlace
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    // Verifica si un usuario ya tiene tokens activos (no usados y no expirados)
    // Ãºtil para evitar spam de correos
    boolean existsByUsuario_UserIdAndUsadoFalse(UUID userId);
}
```

## 

```java
package com.lavarapido.backend_vehicular.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    // JavaMailSender lo provee Spring Boot automÃ¡ticamente
    // con la config de application.properties
    private final JavaMailSender mailSender;

    // URL base del frontend, se define en application.properties
    // ejemplo: app.frontend.url=http://localhost:5173
    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * EnvÃ­a el correo de recuperaciÃ³n de contraseÃ±a.
     * El enlace lleva el token en texto plano (no el hash).
     * El backend hashea al recibirlo para comparar.
     *
     * @param destinatario correo del usuario
     * @param tokenPlano   token generado antes de hashear
     */
    public void enviarCorreoRecuperacion(String destinatario, String tokenPlano) {

        // Arma el enlace que irÃ¡ en el correo
        // ejemplo: http://localhost:5173/reset-password?token=abc123
        String enlace = frontendUrl + "/reset-password?token=" + tokenPlano;

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("RecuperaciÃ³n de contraseÃ±a â€“ Lava RÃ¡pido Vehicular");
        mensaje.setText(
            "Hola,\n\n" +
            "Recibimos una solicitud para restablecer tu contraseÃ±a.\n\n" +
            "Haz clic en el siguiente enlace (vÃ¡lido por 30 minutos):\n" +
            enlace + "\n\n" +
            "Si no solicitaste esto, ignora este correo.\n\n" +
            "â€“ Equipo Lava RÃ¡pido Vehicular"
        );

        mailSender.send(mensaje);
    }
}
```

## 

```java
package com.lavarapido.backend_vehicular.auth.service;

import com.lavarapido.backend_vehicular.auth.entity.TokenRecuperacion;
import com.lavarapido.backend_vehicular.auth.repository.TokenRecuperacionRepository;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository           userRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final EmailService             emailService;
    private final PasswordEncoder          passwordEncoder;

    // Tiempo de vida del token en minutos
    private static final int TTL_MINUTOS = 30;

    // â”€â”€ PASO 1: El usuario pide recuperar su contraseÃ±a â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    /**
     * Busca el usuario por email, genera un token seguro,
     * lo guarda hasheado en BD y envÃ­a el enlace por correo.
     *
     * Si el email no existe no lanza error (por seguridad,
     * no revelamos si el correo estÃ¡ registrado o no).
     */
    @Transactional
    public void solicitarRecuperacion(String email, String ipSolicitante) {

        // Busca el usuario â€” si no existe termina silenciosamente
        userRepository.findByEmail(email).ifPresent(usuario -> {

            // Genera token aleatorio seguro en texto plano
            String tokenPlano = generarTokenSeguro();

            // Hashea el token con SHA-256 para guardarlo en BD
            String tokenHash = hashearSHA256(tokenPlano);

            // Construye y guarda la entidad en tokens_recuperacion
            TokenRecuperacion token = new TokenRecuperacion();
            token.setUsuario(usuario);
            token.setTokenHash(tokenHash);
            token.setExpiracionAt(LocalDateTime.now().plusMinutes(TTL_MINUTOS));
            token.setUsado(false);
            token.setIpSolicitante(ipSolicitante);
            tokenRepository.save(token);

            // EnvÃ­a el correo con el token en texto plano
            emailService.enviarCorreoRecuperacion(email, tokenPlano);
        });
    }

    // â”€â”€ PASO 2: El usuario hace clic en el enlace y manda nueva contraseÃ±a â”€â”€
    /**
     * Valida el token recibido del frontend:
     *   1. Existe en BD (por hash)
     *   2. No ha sido usado
     *   3. No ha expirado
     * Si todo es vÃ¡lido, actualiza la contraseÃ±a y marca el token como usado.
     */
    @Transactional
    public void resetearContrasena(String tokenPlano, String nuevaContrasena) {

        // Hashea el token recibido para buscarlo en BD
        String tokenHash = hashearSHA256(tokenPlano);

        // Busca el token en BD
        TokenRecuperacion token = tokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new RuntimeException("Token invÃ¡lido"));

        // Verifica que no haya sido usado antes
        if (token.getUsado()) {
            throw new RuntimeException("El token ya fue utilizado");
        }

        // Verifica que no haya expirado
        if (LocalDateTime.now().isAfter(token.getExpiracionAt())) {
            throw new RuntimeException("El token ha expirado");
        }

        // Actualiza la contraseÃ±a del usuario con BCrypt
        User usuario = token.getUsuario();
        usuario.setPassword(passwordEncoder.encode(nuevaContrasena));
        userRepository.save(usuario);

        // Marca el token como usado para que no pueda reutilizarse
        token.setUsado(true);
        tokenRepository.save(token);
    }

    // â”€â”€ Utilidades privadas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Genera un token aleatorio seguro de 32 bytes en Base64 URL-safe.
     * SecureRandom es criptogrÃ¡ficamente seguro, no usar Math.random().
     */
    private String generarTokenSeguro() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hashea un String con SHA-256 y retorna el resultado en hexadecimal.
     * El hash tiene exactamente 64 caracteres, coincide con CHAR(64) en BD.
     */
    private String hashearSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre existe en Java, esto nunca deberÃ­a ocurrir
            throw new RuntimeException("Error al hashear el token", e);
        }
    }
}
```

## 

```java
package com.lavarapido.backend_vehicular.roles.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "role_name", length = 20, nullable = false, unique = true)
    private String roleName;

    @Column(name = "description", length = 255)
    private String description;
}
```

## 

```java
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

        // ðŸ”“ PERMITIR PREFLIGHT OPTIONS
        // navegador y Swagger envian OPTIONS antes de POST/PUT
        // si esto se bloquea aparece error 403 aunque la ruta sea publica
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {

            filterChain.doFilter(request, response);
            return;
        }

        // ðŸ” LEER HEADER AUTHORIZATION
        // si no existe Authorization o no inicia con Bearer
        // significa que la ruta puede ser publica (login, registro, swagger)
        final String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null ||
            !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // âœ‚ï¸ EXTRAER TOKEN JWT
        // se elimina el prefijo "Bearer "
        String token = authHeader.substring(7);

        // ðŸ” VALIDAR TOKEN
        // si el token expiro, fue modificado o es invalido
        // se rechaza inmediatamente con 401
        if (!jwtService.isTokenValid(token)) {

            response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
            );

            response.getWriter().write("Token invalido");

            return;
        }

        // ðŸ”¥ EXTRAER EMAIL DEL TOKEN
        // el email identifica al usuario autenticado
        String email =
                jwtService.extractEmail(token);

        // ðŸ”¥ CREAR OBJETO DE AUTENTICACION
        // no usamos password aqui porque JWT ya fue validado
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.emptyList()
                );

        // ðŸ”¥ GUARDAR AUTENTICACION EN SPRING SECURITY
        // desde este punto Spring reconoce al usuario como autenticado
        SecurityContextHolder
                .getContext()
                .setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
```

## 

```java
package com.lavarapido.backend_vehicular.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    // se inyecta la clave secreta desde application.properties por seguridad
    @Value("${jwt.secret}")
    private String secret;

    // genera la clave de firma usando el algoritmo hmac sha a partir del secreto configurado
    private SecretKey getKey() {

        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    // crea un token jwt con una duracion de 1 hora para el usuario autenticado
    public String generateToken(String email) {

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())

                // tiempo de expiracion calculado en milisegundos (1 hora)
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                + 1000L * 60 * 60
                        )
                )

                .signWith(getKey())
                .compact();
    }

    // obtiene el identificador del usuario (subject) contenido en el cuerpo del token
    public String extractEmail(String token) {

        return getClaims(token).getSubject();
    }

    // verifica si el token es estructuralmente valido y no ha expirado
    public boolean isTokenValid(String token) {

        try {

            getClaims(token);

            return true;

        } catch (JwtException | IllegalArgumentException e) {

            // retorna falso si la firma es invalida o el formato es incorrecto
            return false;
        }
    }

    // procesa y valida la firma del token para extraer la carga util (claims)
    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

## 

```java
package com.lavarapido.backend_vehicular.servicios.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lavarapido.backend_vehicular.servicios.dto.ServicioRequestDTO;
import com.lavarapido.backend_vehicular.servicios.dto.ServicioResponseDTO;
import com.lavarapido.backend_vehicular.servicios.service.ServicioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;

    @PostMapping
    public ResponseEntity<ServicioResponseDTO> crear(@Valid @RequestBody ServicioRequestDTO request) {
        ServicioResponseDTO creado = servicioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    public ResponseEntity<List<ServicioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(servicioService.listarTodos());
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<ServicioResponseDTO>> listarDisponibles() {
        return ResponseEntity.ok(servicioService.listarDisponibles());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ServicioResponseDTO>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(servicioService.buscarPorNombre(nombre));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioResponseDTO> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(servicioService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioResponseDTO> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ServicioRequestDTO request) {
        return ResponseEntity.ok(servicioService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ServicioResponseDTO> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam boolean activo) {
        return ResponseEntity.ok(servicioService.cambiarEstado(id, activo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        servicioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

## 

```java
package com.lavarapido.backend_vehicular.servicios.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicioRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 300, message = "La descripcion no puede superar los 300 caracteres")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    @NotNull(message = "La duracion es obligatoria")
    @Min(value = 15, message = "La duracion minima es 15 minutos")
    @Max(value = 180, message = "La duracion maxima es 180 minutos")
    private Integer duracionMinutos;
}
```

## 

```java
package com.lavarapido.backend_vehicular.servicios.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicioResponseDTO {

    private UUID idServicio;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer duracionMinutos;
    private Boolean estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## 

```java
package com.lavarapido.backend_vehicular.servicios.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "servicios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_servicio")
    private UUID idServicio;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 10, scale = 0)
    private BigDecimal precio;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
```

## 

```java
package com.lavarapido.backend_vehicular.servicios.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
public interface ServicioRepository extends JpaRepository<Servicio, UUID> {

    Optional<Servicio> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    List<Servicio> findByEstadoTrue();

    List<Servicio> findByNombreContainingIgnoreCase(String nombre);
}
```

## 

```java
package com.lavarapido.backend_vehicular.servicios.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lavarapido.backend_vehicular.servicios.dto.ServicioRequestDTO;
import com.lavarapido.backend_vehicular.servicios.dto.ServicioResponseDTO;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
import com.lavarapido.backend_vehicular.servicios.repository.ServicioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository servicioRepository;

    public ServicioResponseDTO crear(ServicioRequestDTO request) {
        if (servicioRepository.existsByNombre(request.getNombre())) {
            throw new IllegalArgumentException("Ya existe un servicio con ese nombre");
        }

        Servicio servicio = new Servicio();
        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setPrecio(request.getPrecio());
        servicio.setDuracionMinutos(request.getDuracionMinutos());
        servicio.setEstado(true);

        Servicio guardado = servicioRepository.save(servicio);
        return mapearAResponse(guardado);
    }

    public List<ServicioResponseDTO> listarTodos() {
        return servicioRepository.findAll()
                .stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    public List<ServicioResponseDTO> listarDisponibles() {
        return servicioRepository.findByEstadoTrue()
                .stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    public List<ServicioResponseDTO> buscarPorNombre(String nombre) {
        return servicioRepository.findByNombreContainingIgnoreCase(nombre)
                .stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    public ServicioResponseDTO obtenerPorId(UUID id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));
        return mapearAResponse(servicio);
    }

    public ServicioResponseDTO actualizar(UUID id, ServicioRequestDTO request) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));

        if (!servicio.getNombre().equalsIgnoreCase(request.getNombre())
                && servicioRepository.existsByNombre(request.getNombre())) {
            throw new IllegalArgumentException("Ya existe un servicio con ese nombre");
        }

        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setPrecio(request.getPrecio());
        servicio.setDuracionMinutos(request.getDuracionMinutos());

        Servicio actualizado = servicioRepository.save(servicio);
        return mapearAResponse(actualizado);
    }

    public ServicioResponseDTO cambiarEstado(UUID id, boolean nuevoEstado) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));

        servicio.setEstado(nuevoEstado);
        Servicio actualizado = servicioRepository.save(servicio);
        return mapearAResponse(actualizado);
    }

    public void eliminar(UUID id) {
        cambiarEstado(id, false);
    }

    private ServicioResponseDTO mapearAResponse(Servicio servicio) {
        return new ServicioResponseDTO(
                servicio.getIdServicio(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getPrecio(),
                servicio.getDuracionMinutos(),
                servicio.getEstado(),
                servicio.getCreatedAt(),
                servicio.getUpdatedAt()
        );
    }
}
```

## 

```java
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

            // ðŸ”“ Habilita CORS usando la configuracion definida en WebConfig
            // permite que frontend (React/Vite) y Swagger puedan consumir el backend
            .cors(Customizer.withDefaults())

            // ðŸ”“ Se desactiva CSRF porque este proyecto usa JWT stateless
            // CSRF aplica principalmente cuando se usan sesiones y cookies
            .csrf(csrf -> csrf.disable())

            // ðŸ”’ SIN SESIONES
            // cada request debe autenticarse con JWT
            // Spring no guardara sesiones en servidor
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // ðŸ”“ Se desactiva el formulario de login automatico de Spring
            // evitamos que Spring muestre su login por defecto
            .formLogin(form -> form.disable())

            // ðŸ”“ Se desactiva autenticacion basica tipo navegador
            // no usamos usuario/password por Basic Auth
            .httpBasic(basic -> basic.disable())

            .authorizeHttpRequests(auth -> auth

                // ðŸ”“ RUTAS PUBLICAS
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

                // ðŸ”’ TODO LO DEMAS
                // cualquier otra ruta necesita token JWT valido
                .anyRequest().authenticated()
            )

            // ðŸ” FILTRO JWT PERSONALIZADO
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

        // ðŸ” BCrypt
        // usado para encriptar contrasenas en registro
        // y validar contrasena en login
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {

        // ðŸ”• Evita el warning:
        // Using generated security password
        // no usamos usuarios en memoria realmente
        // solo se declara para evitar el comportamiento por defecto
        return new InMemoryUserDetailsManager();
    }
}
```

## 

```java
package com.lavarapido.backend_vehicular.shared.config;

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

                        // ðŸ”“ ORÃGENES PERMITIDOS
                        // NO usar allowedOrigins("*") junto con allowCredentials(true)
                        // porque Spring lanza IllegalArgumentException
                        // AquÃ­ declaramos explÃ­citamente los orÃ­genes vÃ¡lidos
                        .allowedOrigins(
                                "http://localhost:5173", // React / Frontend
                                "http://localhost:8081" // Swagger / Backend
                                
                        )

                        // ðŸ”“ MÃ‰TODOS HTTP PERMITIDOS
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")

                        // ðŸ”“ HEADERS PERMITIDOS
                        .allowedHeaders(
                                "Authorization",
                                "Content-Type",
                                "Accept"
                        )

                        // ðŸ”“ PERMITIR CREDENCIALES
                        // necesario para JWT / sesiones / auth headers
                        .allowCredentials(true);
            }
        };
    }
}
```

## 

```java
package com.lavarapido.backend_vehicular.shared.enums;

public enum DocumentType {
    CC, TI, CE
}
```

## 

```java
package com.lavarapido.backend_vehicular.users.controller;

import com.lavarapido.backend_vehicular.auth.dto.LoginDTO;
import com.lavarapido.backend_vehicular.auth.dto.LoginResponseDTO;
import com.lavarapido.backend_vehicular.users.dto.UserRegistrationDTO;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // ðŸ”¥ REGISTRO
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationDTO dto) {

        try {
            User user = userService.registerUser(dto);
            return ResponseEntity.ok(user);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ðŸ” LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO dto) {

        try {
            LoginResponseDTO response = userService.login(dto);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/profile")
    public String profile() {

        var auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String email = auth.getName();

        return "Authenticated user: " + email;
    }
}
```

## 

```java
package com.lavarapido.backend_vehicular.users.dto;

import com.lavarapido.backend_vehicular.shared.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDTO {

    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private DocumentType documentType;
    private String documentNumber;
    private String password;
}
```

## 

```java
package com.lavarapido.backend_vehicular.users.entity;

import com.lavarapido.backend_vehicular.shared.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "phone_number", length = 10, nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 10, nullable = false)
    private DocumentType documentType;

    @Column(name = "document_number", length = 12, nullable = false)
    private String documentNumber;

    @Column(name = "password", length = 60, nullable = false)
    private String password;
    @Column(
        name = "profile_picture",
        length = 20,
        nullable = false
    )
    private String profilePicture = "avatar_1";

    @Column(nullable = false)
    private Boolean status = true;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
```

## 

```java
package com.lavarapido.backend_vehicular.users.entity;

import java.time.LocalDateTime;

import com.lavarapido.backend_vehicular.roles.entity.Role;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "fk_user_id")
    private User user;

    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "fk_role_id")
    private Role role;

    @Column(nullable = false)
    private Boolean status = true;

        @Column(name = "assigned_at", insertable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
```

## 

```java
package com.lavarapido.backend_vehicular.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserRoleId implements Serializable {

    @Column(name = "fk_user_id")
    private UUID userId;

    @Column(name = "fk_role_id")
    private UUID roleId;
}
```

## 

```java
package com.lavarapido.backend_vehicular.users.repository;

import com.lavarapido.backend_vehicular.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // search by email (login)
    Optional<User> findByEmail(String email);

    // validate if already exists
    boolean existsByEmail(String email);
}
```

## 

```java
package com.lavarapido.backend_vehicular.users.repository;

import com.lavarapido.backend_vehicular.users.entity.UserRole;
import com.lavarapido.backend_vehicular.users.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    // search active role by user UUID
    @Query("""
        SELECT ur
        FROM UserRole ur
        WHERE ur.user.userId = :userId
        AND ur.status = true
    """)
    Optional<UserRole> findActiveRoleByUserId(@Param("userId") UUID userId);
}
```

## 

```java
package com.lavarapido.backend_vehicular.users.service;

import com.lavarapido.backend_vehicular.auth.dto.LoginDTO;
import com.lavarapido.backend_vehicular.auth.dto.LoginResponseDTO;
import com.lavarapido.backend_vehicular.security.JwtService;
import com.lavarapido.backend_vehicular.users.dto.UserRegistrationDTO;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.users.repository.UserRoleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRoleRepository userRoleRepository;

    // ðŸ”¥ REGISTRO DE USUARIO
    // @Transactional garantiza que si ocurre un error durante el proceso,
    // toda la operacion se revierte automaticamente (rollback)
    // evitando registros incompletos o inconsistentes en base de datos
    @Transactional
    public User registerUser(UserRegistrationDTO dto) {

        // verifica disponibilidad del correo para evitar duplicados
        // antes de intentar guardar en la base de datos
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("el correo ya esta registrado");
        }

        // se crea una nueva instancia de la entidad User
        // que sera persistida en la tabla users
        User user = new User();

        // transferencia manual de datos desde el DTO hacia la entidad
        // este proceso se conoce como mapeo manual
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setDocumentType(dto.getDocumentType());
        user.setDocumentNumber(dto.getDocumentNumber());

        // encriptacion de la contrasena usando BCrypt
        // nunca se debe guardar la contrasena en texto plano
        // el resultado encaja con CHAR(60) en PostgreSQL
        user.setPassword(
            passwordEncoder.encode(dto.getPassword())
        );

        // guarda el usuario en la base de datos
        // Hibernate genera el UUID automaticamente y retorna
        // el objeto persistido con sus datos completos
        return userRepository.save(user);
    }

// ðŸ” LOGIN DE USUARIO
public LoginResponseDTO login(LoginDTO dto) {

    var userOpt = userRepository.findByEmail(dto.getEmail());

    if (userOpt.isEmpty()) {
        throw new RuntimeException("Usuario no encontrado");
    }

    User user = userOpt.get();



    boolean valid = passwordEncoder.matches(
        dto.getPassword(),
        user.getPassword()
    );

    if (!valid) {
        throw new RuntimeException("Contrasena incorrecta");
    }

    var userRoleOpt = userRoleRepository
            .findActiveRoleByUserId(user.getUserId());

    String roleName = userRoleOpt
            .map(ur -> ur.getRole().getRoleName())
            .orElse("USER");

    String token = jwtService.generateToken(user.getEmail());

    LoginResponseDTO.UserInfoDTO info =
            new LoginResponseDTO.UserInfoDTO(
                    user.getUserId().toString(),
                    user.getFirstName(),
                    user.getEmail(),
                    roleName
            );

    return new LoginResponseDTO(token, info);
}
}
```

## 

```java
package com.lavarapido.backend_vehicular;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendVehicularApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendVehicularApplication.class, args);
	}

}
```

