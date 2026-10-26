package com.lavarapido.backend_vehicular.users.repository;

import com.lavarapido.backend_vehicular.users.entity.UserRole;
import com.lavarapido.backend_vehicular.users.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

/** Consulta las asignaciones de rol activas de una cuenta. */
public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    // AccountAccessService usa solo roles vigentes para decidir permisos.
    List<UserRole> findAllByUser_UserIdAndStatusTrue(UUID userId);
}
