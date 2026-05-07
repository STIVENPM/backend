package com.lavarapido.backend_vehicular.repository;

import com.lavarapido.backend_vehicular.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // search by email (login)
    Optional<User> findByEmail(String email);

    // validate if already exists
    boolean existsByEmail(String email);
}