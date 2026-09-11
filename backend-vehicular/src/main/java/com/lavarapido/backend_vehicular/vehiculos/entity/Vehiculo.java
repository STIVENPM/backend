
package com.lavarapido.backend_vehicular.vehiculos.entity;

import com.lavarapido.backend_vehicular.marcas.entity.Marca;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;

import jakarta.persistence.*;


import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehiculos")
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

    public Vehiculo() { }
    public Vehiculo(UUID idVehiculo, User usuario, Marca marca, String placa, String color, TipoVehiculo tipoVehiculo, Boolean estado, LocalDateTime createdAt, LocalDateTime updatedAt) { this.idVehiculo=idVehiculo; this.usuario=usuario; this.marca=marca; this.placa=placa; this.color=color; this.tipoVehiculo=tipoVehiculo; this.estado=estado; this.createdAt=createdAt; this.updatedAt=updatedAt; }
    public UUID getIdVehiculo(){return idVehiculo;} public void setIdVehiculo(UUID v){idVehiculo=v;}
    public User getUsuario(){return usuario;} public void setUsuario(User v){usuario=v;}
    public Marca getMarca(){return marca;} public void setMarca(Marca v){marca=v;}
    public String getPlaca(){return placa;} public void setPlaca(String v){placa=v;}
    public String getColor(){return color;} public void setColor(String v){color=v;}
    public TipoVehiculo getTipoVehiculo(){return tipoVehiculo;} public void setTipoVehiculo(TipoVehiculo v){tipoVehiculo=v;}
    public Boolean getEstado(){return estado;} public void setEstado(Boolean v){estado=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}


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

