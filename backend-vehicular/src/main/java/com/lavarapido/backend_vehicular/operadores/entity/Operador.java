package com.lavarapido.backend_vehicular.operadores.entity;

import com.lavarapido.backend_vehicular.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "operadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operador {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_operador", nullable = false, updatable = false)
    private UUID idOperador;

    @OneToOne(optional = false)
    @JoinColumn(name = "fk_id_usuario", nullable = false, unique = true)
    private User usuario;

    @Column(name = "estado", nullable = false)
    @Builder.Default
    private Boolean estado = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = ahora;
        }
        updatedAt = ahora;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
