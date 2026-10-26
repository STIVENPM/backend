package com.lavarapido.backend_vehicular.auditoria.entity;
import com.lavarapido.backend_vehicular.auditoria.enums.*;
import com.lavarapido.backend_vehicular.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity @Table(name = "auditoria") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Auditoria {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "id_auditoria", nullable = false, updatable = false) private UUID idAuditoria;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fk_id_usuario") private User usuario;
    @Enumerated(EnumType.STRING) @Column(name = "accion", nullable = false, length = 30) private AccionAuditoria accion;
    @Column(name = "descripcion", length = 500) private String descripcion;
    @Column(name = "ip_origen", length = 45) private String ipOrigen;
    @Enumerated(EnumType.STRING) @Column(name = "modulo", length = 20) private ModuloAuditoria modulo;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
