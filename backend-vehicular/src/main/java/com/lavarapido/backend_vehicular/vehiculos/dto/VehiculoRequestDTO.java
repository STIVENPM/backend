package com.lavarapido.backend_vehicular.vehiculos.dto;


import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Recibe placa, color, tipo y marca del vehículo que se quiere registrar o actualizar. */
public record VehiculoRequestDTO(

    // Se valida junto con tipoVehiculo y se normaliza en PlacaVehiculoValidator.
    @NotNull(message = "La placa es obligatoria")
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
