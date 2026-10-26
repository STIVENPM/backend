package com.lavarapido.backend_vehicular.notificaciones.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificacion_entregas")
public class NotificacionEntrega {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_entrega") public UUID id;
    @Column(name = "fk_id_notificacion", nullable = false) public UUID notificacionId;
    @Column(nullable = false, length = 10) public String canal;
    @Column(name = "fk_id_dispositivo") public UUID dispositivoId;
    @Column(nullable = false, length = 12) public String estado = "pendiente";
    @Column(nullable = false) public int intentos;
    @Column(name = "proveedor_id", length = 100) public String proveedorId;
    @Column(name = "ultimo_error", length = 255) public String ultimoError;
    @Column(name = "proxima_ejecucion", nullable = false) public LocalDateTime proximaEjecucion;
    @Column(name = "enviada_at") public LocalDateTime enviadaAt;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
    @PrePersist void create() { createdAt = LocalDateTime.now(); updatedAt = createdAt; if (proximaEjecucion == null) proximaEjecucion = createdAt; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
