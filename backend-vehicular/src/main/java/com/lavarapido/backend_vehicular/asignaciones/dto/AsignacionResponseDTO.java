package com.lavarapido.backend_vehicular.asignaciones.dto;

import com.lavarapido.backend_vehicular.asignaciones.enums.EstadoAsignacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record AsignacionResponseDTO(

        // =========================================================
        // ASIGNACIÓN
        // =========================================================

        UUID idAsignacion,
        UUID idReserva,
        UUID idOperador,
        String nombreOperador,
        EstadoAsignacion estado,
        LocalDateTime fechaAsignacion,

        // =========================================================
        // CLIENTE
        // =========================================================

        String nombreCliente,

        // =========================================================
        // VEHÍCULO
        // =========================================================

        String placa,
        String color,
        String tipoVehiculo,

        // =========================================================
        // SERVICIO
        // =========================================================

        UUID idServicio,
        String nombreServicio,
        String descripcionServicio,
        BigDecimal precioServicio,
        Integer duracionMinutos,

        // =========================================================
        // RESERVA
        // =========================================================

        LocalDate fechaReserva,
        LocalTime horaReserva,

        // =========================================================
        // AUDITORÍA
        // =========================================================

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
