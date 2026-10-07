package com.lavarapido.backend_vehicular.servicios.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
import jakarta.persistence.LockModeType;
public interface ServicioRepository extends JpaRepository<Servicio, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Servicio s where s.idServicio = :id")
    Optional<Servicio> findByIdForUpdate(@Param("id") UUID id);

    Optional<Servicio> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    List<Servicio> findByEstadoTrue();

    List<Servicio> findByNombreContainingIgnoreCase(String nombre);
}
