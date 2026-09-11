package com.lavarapido.backend_vehicular.reservas.dto;

import com.lavarapido.backend_vehicular.reservas.enums.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

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
    public ReservaResponseDTO() { }
    public ReservaResponseDTO(UUID idReserva, UUID idUsuario, String nombreUsuario, UUID idVehiculo, String placaVehiculo, String tipoVehiculo, UUID idServicio, String nombreServicio, String descripcionServicio, BigDecimal precioServicio, Integer duracionServicio, LocalDate fechaReserva, LocalTime horaReserva, LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, EstadoReserva estado, LocalDateTime createdAt, LocalDateTime updatedAt) { this.idReserva=idReserva;this.idUsuario=idUsuario;this.nombreUsuario=nombreUsuario;this.idVehiculo=idVehiculo;this.placaVehiculo=placaVehiculo;this.tipoVehiculo=tipoVehiculo;this.idServicio=idServicio;this.nombreServicio=nombreServicio;this.descripcionServicio=descripcionServicio;this.precioServicio=precioServicio;this.duracionServicio=duracionServicio;this.fechaReserva=fechaReserva;this.horaReserva=horaReserva;this.fechaHoraInicio=fechaHoraInicio;this.fechaHoraFin=fechaHoraFin;this.estado=estado;this.createdAt=createdAt;this.updatedAt=updatedAt; }
    public UUID getIdReserva(){return idReserva;} public void setIdReserva(UUID v){idReserva=v;} public UUID getIdUsuario(){return idUsuario;} public void setIdUsuario(UUID v){idUsuario=v;} public String getNombreUsuario(){return nombreUsuario;} public void setNombreUsuario(String v){nombreUsuario=v;} public UUID getIdVehiculo(){return idVehiculo;} public void setIdVehiculo(UUID v){idVehiculo=v;} public String getPlacaVehiculo(){return placaVehiculo;} public void setPlacaVehiculo(String v){placaVehiculo=v;} public String getTipoVehiculo(){return tipoVehiculo;} public void setTipoVehiculo(String v){tipoVehiculo=v;} public UUID getIdServicio(){return idServicio;} public void setIdServicio(UUID v){idServicio=v;} public String getNombreServicio(){return nombreServicio;} public void setNombreServicio(String v){nombreServicio=v;} public String getDescripcionServicio(){return descripcionServicio;} public void setDescripcionServicio(String v){descripcionServicio=v;} public BigDecimal getPrecioServicio(){return precioServicio;} public void setPrecioServicio(BigDecimal v){precioServicio=v;} public Integer getDuracionServicio(){return duracionServicio;} public void setDuracionServicio(Integer v){duracionServicio=v;} public LocalDate getFechaReserva(){return fechaReserva;} public void setFechaReserva(LocalDate v){fechaReserva=v;} public LocalTime getHoraReserva(){return horaReserva;} public void setHoraReserva(LocalTime v){horaReserva=v;} public LocalDateTime getFechaHoraInicio(){return fechaHoraInicio;} public void setFechaHoraInicio(LocalDateTime v){fechaHoraInicio=v;} public LocalDateTime getFechaHoraFin(){return fechaHoraFin;} public void setFechaHoraFin(LocalDateTime v){fechaHoraFin=v;} public EstadoReserva getEstado(){return estado;} public void setEstado(EstadoReserva v){estado=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
