package com.lavarapido.backend_vehicular.reservas.dto;

import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaResponseDTO {

    private UUID idReserva;
    private UUID idUsuario;
    private String nombreUsuario;
    private UUID idVehiculo;
    private String placaVehiculo;
    private String tipoVehiculo;
    private UUID idServicio;
    private String nombreServicio;
    private String descripcionServicio;
    private BigDecimal precioServicio;
    private Integer duracionServicio;
    private LocalDate fechaReserva;
    private LocalTime horaReserva;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private EstadoReserva estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
