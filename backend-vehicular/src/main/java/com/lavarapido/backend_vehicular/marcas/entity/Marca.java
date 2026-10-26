package com.lavarapido.backend_vehicular.marcas.entity;


import com.lavarapido.backend_vehicular.users.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "marcas")
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_marca")
    private UUID idMarca;

    @Column(name = "nombre", length = 30, nullable = false, unique = true)
    private String nombre;

    // TRUE = aprobada/visible en el catálogo. FALSE = pendiente de revisión
    // (sugerida por un cliente al registrar un vehículo con marca nueva).
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    // Usuario que sugirió la marca (nullable: NULL si la creó el admin
    // directamente, o si el usuario que la sugirió borró su cuenta).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_id_usuario_solicitante")
    private User usuarioSolicitante;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Marca() { }
    public Marca(UUID idMarca, String nombre, Boolean estado, User usuarioSolicitante, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idMarca = idMarca; this.nombre = nombre; this.estado = estado; this.usuarioSolicitante = usuarioSolicitante; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }
    public UUID getIdMarca() { return idMarca; }
    public void setIdMarca(UUID idMarca) { this.idMarca = idMarca; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }
    public User getUsuarioSolicitante() { return usuarioSolicitante; }
    public void setUsuarioSolicitante(User usuarioSolicitante) { this.usuarioSolicitante = usuarioSolicitante; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

}
