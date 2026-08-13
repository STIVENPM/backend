package com.lavarapido.backend_vehicular.vehiculos.dto;


import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VehiculoRequestDTO(

    // Placa: acepta formato antiguo (ABC123) o nuevo (ABC12A).
    // Se normaliza a mayúsculas en el service antes de guardar.
    @NotNull(message = "La placa es obligatoria")
    @Pattern(
        regexp = "^[A-Za-z]{3}[0-9]{3}$|^[A-Za-z]{3}[0-9]{2}[A-Za-z]{1}$",
        message = "La placa debe tener el formato ABC123 o ABC12A"
    )
    String placa,

    @Size(max = 30, message = "El color no puede superar los 30 caracteres")
    String color,

    @NotNull(message = "El tipo de vehículo es obligatorio")
    TipoVehiculo tipoVehiculo,

    // Marca del catálogo aprobado (/api/marcas/activas). Si el cliente
    // no encuentra la suya, debe pedirle al admin que la agregue desde
    // el panel — ya no puede sugerir una marca nueva desde aquí.
    @NotNull(message = "Debes seleccionar una marca del catálogo")
    UUID fkIdMarca

) {}