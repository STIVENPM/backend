package com.lavarapido.backend_vehicular.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class UserRoleId implements Serializable {

    @Column(name = "fk_user_id")
    private UUID userId;

    @Column(name = "fk_role_id")
    private UUID roleId;
    public UserRoleId() { }
    public UserRoleId(UUID userId, UUID roleId) { this.userId=userId; this.roleId=roleId; }
    public UUID getUserId(){return userId;} public void setUserId(UUID v){userId=v;} public UUID getRoleId(){return roleId;} public void setRoleId(UUID v){roleId=v;}
    protected boolean canEqual(Object other) { return other instanceof UserRoleId; }
    @Override public boolean equals(Object o) { if (o == this) return true; if (!(o instanceof UserRoleId other)) return false; if (!other.canEqual(this)) return false; return java.util.Objects.equals(userId, other.userId) && java.util.Objects.equals(roleId, other.roleId); }
    @Override public int hashCode() { int result=1; result=result*59+(userId==null?43:userId.hashCode()); result=result*59+(roleId==null?43:roleId.hashCode()); return result; }
}

