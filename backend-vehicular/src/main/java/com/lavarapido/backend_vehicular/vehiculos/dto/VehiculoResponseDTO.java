package com.lavarapido.backend_vehicular.vehiculos.dto;


import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;

import java.time.LocalDateTime;
import java.util.UUID;

public record VehiculoResponseDTO(
    UUID idVehiculo,
    UUID userId,
    // Datos del dueño, para que el panel admin no tenga que resolver
    // el UUID contra /api/users por cada fila de la tabla.
    String nombreUsuario,
    String emailUsuario,
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