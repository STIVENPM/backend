package com.lavarapido.backend_vehicular.calificaciones.entity;

import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import com.lavarapido.backend_vehicular.users.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "calificaciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Calificacion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_calificacion", nullable = false, updatable = false)
    private UUID idCalificacion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_reserva", nullable = false, unique = true)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_usuario", nullable = false)
    private User usuario;

    @Min(value = 1, message = "La puntuacion debe estar entre 1 y 5")
    @Max(value = 5, message = "La puntuacion debe estar entre 1 y 5")
    @Column(name = "puntuacion", nullable = false)
    private Integer puntuacion;

    @Column(name = "comentario", length = 300)
    private String comentario;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { LocalDateTime now = LocalDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
