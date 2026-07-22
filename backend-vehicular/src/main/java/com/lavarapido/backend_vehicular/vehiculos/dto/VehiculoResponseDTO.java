package com.lavarapido.backend_vehicular.vehiculos.dto;


import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;

import java.time.LocalDateTime;
import java.util.UUID;

public record VehiculoResponseDTO(
    UUID idVehiculo,
    UUID userId,
    UUID idMarca,
    String nombreMarca,
    // Informa al frontend si la marca todavía no fue aprobada por el
    // admin — útil para mostrar un aviso tipo "marca en revisión".
    Boolean marcaAprobada,
    String placa,
    String color,
    TipoVehiculo tipoVehiculo,
    Boolean estado,
    LocalDateTime createdAt
) {}