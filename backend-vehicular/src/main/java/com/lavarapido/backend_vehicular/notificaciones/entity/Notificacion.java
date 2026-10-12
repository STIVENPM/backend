package com.lavarapido.backend_vehicular.notificaciones.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificaciones", uniqueConstraints = @UniqueConstraint(columnNames = {"fk_id_usuario", "clave_evento"}))
public class Notificacion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_notificacion") public UUID id;
    @Column(name = "fk_id_usuario", nullable = false) public UUID usuarioId;
    @Column(name = "clave_evento", nullable = false, length = 180) public String claveEvento;
    @Column(nullable = false, length = 30) public String tipo;
    @Column(nullable = false, length = 160) public String titulo;
    @Column(nullable = false, length = 500) public String mensaje;
    @Column(name = "fk_id_reserva") public UUID reservaId;
    @Column(name = "fk_id_pago") public UUID pagoId;
    @Column(name = "leida_at") public LocalDateTime leidaAt;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @PrePersist void create() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
