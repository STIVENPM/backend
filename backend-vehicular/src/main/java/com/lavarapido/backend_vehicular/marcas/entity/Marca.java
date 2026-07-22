package com.lavarapido.backend_vehicular.marcas.entity;


import com.lavarapido.backend_vehicular.users.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "marcas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
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

}