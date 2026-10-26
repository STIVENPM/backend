package com.lavarapido.backend_vehicular.users.entity;

import java.time.LocalDateTime;

import com.lavarapido.backend_vehicular.roles.entity.Role;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_roles")
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
    public UserRole() { }
    public UserRole(UserRoleId id, User user, Role role, Boolean status, LocalDateTime assignedAt, LocalDateTime revokedAt, LocalDateTime createdAt, LocalDateTime updatedAt) { this.id=id; this.user=user; this.role=role; this.status=status; this.assignedAt=assignedAt; this.revokedAt=revokedAt; this.createdAt=createdAt; this.updatedAt=updatedAt; }
    public UserRoleId getId(){return id;} public void setId(UserRoleId v){id=v;} public User getUser(){return user;} public void setUser(User v){user=v;} public Role getRole(){return role;} public void setRole(Role v){role=v;} public Boolean getStatus(){return status;} public void setStatus(Boolean v){status=v;} public LocalDateTime getAssignedAt(){return assignedAt;} public void setAssignedAt(LocalDateTime v){assignedAt=v;} public LocalDateTime getRevokedAt(){return revokedAt;} public void setRevokedAt(LocalDateTime v){revokedAt=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
