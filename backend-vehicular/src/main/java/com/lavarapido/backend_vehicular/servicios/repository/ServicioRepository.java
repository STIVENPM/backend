package com.lavarapido.backend_vehicular.servicios.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
public interface ServicioRepository extends JpaRepository<Servicio, UUID> {

    Optional<Servicio> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    List<Servicio> findByEstadoTrue();

    List<Servicio> findByNombreContainingIgnoreCase(String nombre);
}