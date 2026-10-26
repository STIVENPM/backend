package com.lavarapido.backend_vehicular.notificaciones.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dispositivos_push")
public class DispositivoPush {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_dispositivo") public UUID id;
    @Column(name = "fk_id_usuario", nullable = false) public UUID usuarioId;
    @Column(name = "instalacion_id", nullable = false, length = 100) public String instalacionId;
    @Column(nullable = false, length = 255) public String token;
    @Column(nullable = false) public boolean activo = true;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
    @PrePersist void create() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
