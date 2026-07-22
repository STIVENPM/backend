package com.lavarapido.backend_vehicular.marcas.repository;


import com.lavarapido.backend_vehicular.marcas.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarcaRepository extends JpaRepository<Marca, UUID> {

    // Para validar unicidad al crear/sugerir una marca (evita duplicados
    // por mayúsculas/minúsculas, ej. "Toyota" vs "TOYOTA").
    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Marca> findByNombreIgnoreCase(String nombre);

    // Catálogo aprobado — lo que ve el cliente en el <select> al
    // registrar su vehículo.
    List<Marca> findByEstadoTrue();

    // Solicitudes pendientes por revisar — panel del admin.
    List<Marca> findByEstadoFalse();

    // Búsqueda parcial para el admin (ej. barra de búsqueda de marcas).
    List<Marca> findByNombreContainingIgnoreCase(String nombre);
}