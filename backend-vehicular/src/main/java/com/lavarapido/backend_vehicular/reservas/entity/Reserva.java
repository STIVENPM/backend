package com.lavarapido.backend_vehicular.reservas.entity;

import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;
import com.lavarapido.backend_vehicular.servicios.entity.Servicio;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.vehiculos.entity.Vehiculo;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

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
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Reserva() { }
    public Reserva(UUID idReserva, User usuario, Vehiculo vehiculo, Servicio servicio, LocalDate fechaReserva, LocalTime horaReserva, LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, EstadoReserva estado, LocalDateTime createdAt, LocalDateTime updatedAt) { this.idReserva=idReserva;this.usuario=usuario;this.vehiculo=vehiculo;this.servicio=servicio;this.fechaReserva=fechaReserva;this.horaReserva=horaReserva;this.fechaHoraInicio=fechaHoraInicio;this.fechaHoraFin=fechaHoraFin;this.estado=estado;this.createdAt=createdAt;this.updatedAt=updatedAt; }
    public UUID getIdReserva(){return idReserva;} public void setIdReserva(UUID v){idReserva=v;} public User getUsuario(){return usuario;} public void setUsuario(User v){usuario=v;} public Vehiculo getVehiculo(){return vehiculo;} public void setVehiculo(Vehiculo v){vehiculo=v;} public Servicio getServicio(){return servicio;} public void setServicio(Servicio v){servicio=v;} public LocalDate getFechaReserva(){return fechaReserva;} public void setFechaReserva(LocalDate v){fechaReserva=v;} public LocalTime getHoraReserva(){return horaReserva;} public void setHoraReserva(LocalTime v){horaReserva=v;} public LocalDateTime getFechaHoraInicio(){return fechaHoraInicio;} public void setFechaHoraInicio(LocalDateTime v){fechaHoraInicio=v;} public LocalDateTime getFechaHoraFin(){return fechaHoraFin;} public void setFechaHoraFin(LocalDateTime v){fechaHoraFin=v;} public EstadoReserva getEstado(){return estado;} public void setEstado(EstadoReserva v){estado=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
    public static Builder builder(){return new Builder();}
    public static class Builder { private UUID idReserva; private User usuario; private Vehiculo vehiculo; private Servicio servicio; private LocalDate fechaReserva; private LocalTime horaReserva; private LocalDateTime fechaHoraInicio; private LocalDateTime fechaHoraFin; private EstadoReserva estado=EstadoReserva.PENDIENTE; private LocalDateTime createdAt; private LocalDateTime updatedAt;
        public Builder idReserva(UUID v){idReserva=v;return this;} public Builder usuario(User v){usuario=v;return this;} public Builder vehiculo(Vehiculo v){vehiculo=v;return this;} public Builder servicio(Servicio v){servicio=v;return this;} public Builder fechaReserva(LocalDate v){fechaReserva=v;return this;} public Builder horaReserva(LocalTime v){horaReserva=v;return this;} public Builder fechaHoraInicio(LocalDateTime v){fechaHoraInicio=v;return this;} public Builder fechaHoraFin(LocalDateTime v){fechaHoraFin=v;return this;} public Builder estado(EstadoReserva v){estado=v;return this;} public Builder createdAt(LocalDateTime v){createdAt=v;return this;} public Builder updatedAt(LocalDateTime v){updatedAt=v;return this;} public Reserva build(){return new Reserva(idReserva,usuario,vehiculo,servicio,fechaReserva,horaReserva,fechaHoraInicio,fechaHoraFin,estado,createdAt,updatedAt);} }

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
