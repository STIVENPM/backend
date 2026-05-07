package com.lavarapido.backend_vehicular.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserRoleId implements Serializable {

    @Column(name = "fk_user_id")
    private UUID userId;

    @Column(name = "fk_role_id")
    private UUID roleId;
}

