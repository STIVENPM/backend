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

    // ── Marca: SOLO UNO de los dos campos debe venir, nunca ambos ni ninguno ──

    // Caso 1: el cliente elige una marca ya existente del catálogo.
    UUID fkIdMarca,

    // Caso 2: el cliente no encuentra su marca y sugiere una nueva.
    // El service la crea en `marcas` con estado = false (pendiente de
    // aprobación) y usa ese id para el vehículo.
    @Size(max = 30, message = "El nombre de la marca no puede superar los 30 caracteres")
    String marcaSugerida

) {}
