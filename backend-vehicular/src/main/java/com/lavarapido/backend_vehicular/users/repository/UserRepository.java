package com.lavarapido.backend_vehicular.users.repository;

import com.lavarapido.backend_vehicular.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // search by email (login)
    Optional<User> findByEmail(String email);

    // validate if already exists
    boolean existsByEmail(String email);

    @Query("select distinct ur.user from UserRole ur where ur.status = true and ur.role.roleName = 'ADMIN' and ur.user.status = true")
    List<User> findActiveAdministratorCandidates();
}
