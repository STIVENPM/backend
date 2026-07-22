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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_vehiculo")
    private UUID idVehiculo;

    // Dueño del vehículo. Se asigna siempre desde el usuario autenticado
    // (JWT) en el service — nunca se recibe directamente del body del DTO.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_usuario", nullable = false)
    private User usuario;

    // Referencia al catálogo de marcas. Nunca NULL: si el cliente sugiere
    // una marca nueva, primero se crea en `marcas` (estado = false) y
    // luego el vehículo apunta a ese registro recién creado.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_marca", nullable = false)
    private Marca marca;

    @Column(name = "placa", length = 7, nullable = false, unique = true)
    private String placa;

    @Column(name = "color", length = 30)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", length = 10, nullable = false)
    private TipoVehiculo tipoVehiculo;

    @Column(name = "estado", nullable = false)
    @Builder.Default
    private Boolean estado = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}