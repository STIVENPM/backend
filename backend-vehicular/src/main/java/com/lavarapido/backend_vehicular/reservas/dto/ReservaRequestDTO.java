package com.lavarapido.backend_vehicular.reservas.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaRequestDTO {

    @NotNull(message = "El vehículo es obligatorio")
    private UUID fkIdVehiculo;

    @NotNull(message = "El servicio es obligatorio")
    private UUID fkIdServicio;

    @NotNull(message = "La fecha de la reserva es obligatoria")
    private LocalDate fechaReserva;

    @NotNull(message = "La hora de la reserva es obligatoria")
    private LocalTime horaReserva;
}
