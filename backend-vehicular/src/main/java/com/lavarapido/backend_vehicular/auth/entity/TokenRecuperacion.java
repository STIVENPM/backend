package com.lavarapido.backend_vehicular.auth.entity;

import com.lavarapido.backend_vehicular.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

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

    // FK hacia users(user_id) — relación ManyToOne
    // Un usuario puede tener varios tokens históricos
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

    // IP opcional de quien hizo la solicitud (auditoría)
    @Column(name = "ip_solicitante", length = 45)
    private String ipSolicitante;

    // Lo pone la BD automáticamente, Java solo lo lee
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
