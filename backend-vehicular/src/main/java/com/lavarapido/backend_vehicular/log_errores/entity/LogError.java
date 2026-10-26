package com.lavarapido.backend_vehicular.log_errores.entity;
import com.lavarapido.backend_vehicular.log_errores.enums.TipoError;
import com.lavarapido.backend_vehicular.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity @Table(name = "log_errores") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LogError {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "id_error", nullable = false, updatable = false) private UUID idError;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fk_id_usuario") private User usuario;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_error", nullable = false, length = 30) private TipoError tipoError;
    @Column(name = "descripcion", length = 500) private String descripcion;
    @Column(name = "ip_origen", length = 45) private String ipOrigen;
    @Builder.Default @Column(name = "resuelto", nullable = false) private Boolean resuelto = false;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { LocalDateTime now = LocalDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
