package com.lavarapido.backend_vehicular.reservas.entity;

import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.vehiculos.entity.Vehiculo;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
    name = "reservas",
    indexes = {
        @Index(name = "idx_reservas_usuario", columnList = "fk_id_usuario"),
        @Index(name = "idx_reservas_vehiculo", columnList = "fk_id_vehiculo"),
        @Index(name = "idx_reservas_servicio", columnList = "fk_id_servicio"),
        @Index(name = "idx_reservas_fecha_estado", columnList = "fecha_reserva, estado")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_reserva", nullable = false, updatable = false)
    private UUID idReserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_usuario", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_vehiculo", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_servicio", nullable = false)
    private Servicio servicio;

    @NotNull(message = "La fecha de la reserva es obligatoria")
    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    @NotNull(message = "La hora de la reserva es obligatoria")
    @Column(name = "hora_reserva", nullable = false)
    private LocalTime horaReserva;

    @Column(name = "fecha_hora_inicio")
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin")
    private LocalDateTime fechaHoraFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = ahora;
        }
        this.updatedAt = ahora;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @AssertTrue(message = "La hora de la reserva debe estar entre 06:00 y 20:00")
    public boolean isHoraReservaValida() {
        return horaReserva != null
                && !horaReserva.isBefore(LocalTime.of(6, 0))
                && !horaReserva.isAfter(LocalTime.of(20, 0));
    }

    @AssertTrue(message = "La fecha de finalización debe ser posterior a la fecha de inicio")
    public boolean isFechasLogicasValidas() {
        if (fechaHoraInicio == null || fechaHoraFin == null) {
            return true;
        }
        return fechaHoraFin.isAfter(fechaHoraInicio);
    }
}
