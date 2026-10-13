package com.lavarapido.backend_vehicular.users.repository;

import com.lavarapido.backend_vehicular.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/** Consulta cuentas por correo y candidatos a administrador activos. */
public interface UserRepository extends JpaRepository<User, UUID> {

    // El login busca la cuenta por el correo recibido.
    Optional<User> findByEmail(String email);

    // El registro comprueba si el correo ya está ocupado.
    boolean existsByEmail(String email);

    // Devuelve cuentas activas con asignación ADMIN activa; la autorización final
    // del UUID se comprueba aparte en AdminIdentityPolicy.
    @Query("select distinct ur.user from UserRole ur where ur.status = true and ur.role.roleName = 'ADMIN' and ur.user.status = true")
    List<User> findActiveAdministratorCandidates();
}
