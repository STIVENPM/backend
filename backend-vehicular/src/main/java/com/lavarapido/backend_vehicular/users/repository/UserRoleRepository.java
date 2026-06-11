package com.lavarapido.backend_vehicular.users.repository;

import com.lavarapido.backend_vehicular.users.entity.UserRole;
import com.lavarapido.backend_vehicular.users.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    // search active role by user UUID
    @Query("""
        SELECT ur
        FROM UserRole ur
        WHERE ur.user.userId = :userId
        AND ur.status = true
    """)
    Optional<UserRole> findActiveRoleByUserId(@Param("userId") UUID userId);
}
