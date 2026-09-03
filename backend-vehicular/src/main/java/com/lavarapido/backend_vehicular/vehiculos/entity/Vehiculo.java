
package com.lavarapido.backend_vehicular.vehiculos.entity;

import com.lavarapido.backend_vehicular.marcas.entity.Marca;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_vehiculo")
    private UUID idVehiculo;


    // =========================================================
    // USUARIO PROPIETARIO
    // =========================================================

    // Dueño del vehículo.
    // Se asigna siempre desde el usuario autenticado mediante JWT.
    // Nunca se recibe directamente desde el DTO.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "fk_id_usuario",
        nullable = false
    )
    private User usuario;


    // =========================================================
    // MARCA
    // =========================================================

    // Referencia al catálogo de marcas.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "fk_id_marca",
        nullable = false
    )
    private Marca marca;


    // =========================================================
    // PLACA
    // =========================================================

    @Column(
        name = "placa",
        length = 7,
        nullable = false,
        unique = true
    )
    private String placa;


    // =========================================================
    // COLOR
    // =========================================================

    @Column(
        name = "color",
        length = 30
    )
    private String color;


    // =========================================================
    // TIPO DE VEHÍCULO
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
        name = "tipo_vehiculo",
        length = 10,
        nullable = false
    )
    private TipoVehiculo tipoVehiculo;


    // =========================================================
    // ESTADO
    // =========================================================

    @Column(
        name = "estado",
        nullable = false
    )
    @Builder.Default
    private Boolean estado = true;


    // =========================================================
    // AUDITORÍA - FECHA DE CREACIÓN
    // =========================================================

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;


    // =========================================================
    // AUDITORÍA - FECHA DE ACTUALIZACIÓN
    // =========================================================

    @Column(
        name = "updated_at",
        nullable = false
    )
    private LocalDateTime updatedAt;


    // =========================================================
    // ANTES DE INSERTAR
    // =========================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (estado == null) {
            estado = true;
        }
    }


    // =========================================================
    // ANTES DE ACTUALIZAR
    // =========================================================

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}

