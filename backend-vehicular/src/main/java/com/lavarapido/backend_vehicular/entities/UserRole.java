package com.lavarapido.backend_vehicular.entities;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "fk_user_id")
    private User user;

    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "fk_role_id")
    private Role role;

    @Column(nullable = false)
    private Boolean status = true;

        @Column(name = "assigned_at", insertable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}