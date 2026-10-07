package com.lavarapido.backend_vehicular.vehiculos.repository;

import com.lavarapido.backend_vehicular.vehiculos.entity.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface VehiculoRepository extends JpaRepository<Vehiculo, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehiculo v where v.idVehiculo = :id")
    Optional<Vehiculo> findByIdForUpdate(@Param("id") UUID id);

    // Validar unicidad de placa al crear/actualizar.
    boolean existsByPlaca(String placa);

    Optional<Vehiculo> findByPlaca(String placa);

    // Vehículos del usuario autenticado (app móvil — "mis vehículos").
    List<Vehiculo> findByUsuario_UserId(UUID userId);

    // Solo los activos del usuario (borrado lógico aplicado).
    List<Vehiculo> findByUsuario_UserIdAndEstadoTrue(UUID userId);
}

