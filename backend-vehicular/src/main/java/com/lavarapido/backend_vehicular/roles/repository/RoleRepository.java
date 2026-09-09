package com.lavarapido.backend_vehicular.roles.repository;

import com.lavarapido.backend_vehicular.roles.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByRoleName(String roleName);
}
