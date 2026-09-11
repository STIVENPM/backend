package com.lavarapido.backend_vehicular.reservas.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class ReservaRequestDTO {

    @NotNull(message = "El vehículo es obligatorio")
    private UUID fkIdVehiculo;

    @NotNull(message = "El servicio es obligatorio")
    private UUID fkIdServicio;

    @NotNull(message = "La fecha de la reserva es obligatoria")
    private LocalDate fechaReserva;

    @NotNull(message = "La hora de la reserva es obligatoria")
    private LocalTime horaReserva;
    public ReservaRequestDTO() { } public ReservaRequestDTO(UUID fkIdVehiculo, UUID fkIdServicio, LocalDate fechaReserva, LocalTime horaReserva) { this.fkIdVehiculo=fkIdVehiculo;this.fkIdServicio=fkIdServicio;this.fechaReserva=fechaReserva;this.horaReserva=horaReserva; }
    public UUID getFkIdVehiculo(){return fkIdVehiculo;} public void setFkIdVehiculo(UUID v){fkIdVehiculo=v;} public UUID getFkIdServicio(){return fkIdServicio;} public void setFkIdServicio(UUID v){fkIdServicio=v;} public LocalDate getFechaReserva(){return fechaReserva;} public void setFechaReserva(LocalDate v){fechaReserva=v;} public LocalTime getHoraReserva(){return horaReserva;} public void setHoraReserva(LocalTime v){horaReserva=v;}
}
