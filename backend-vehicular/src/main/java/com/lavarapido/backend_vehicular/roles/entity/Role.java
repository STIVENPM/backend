package com.lavarapido.backend_vehicular.roles.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "role_name", length = 20, nullable = false, unique = true)
    private String roleName;

    @Column(name = "description", length = 255)
    private String description;
}
