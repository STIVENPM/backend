package com.lavarapido.backend_vehicular.servicios.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


public class ServicioResponseDTO {

    private UUID idServicio;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer duracionMinutos;
    private Boolean estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public ServicioResponseDTO() { }
    public ServicioResponseDTO(UUID idServicio, String nombre, String descripcion, BigDecimal precio, Integer duracionMinutos, Boolean estado, LocalDateTime createdAt, LocalDateTime updatedAt) { this.idServicio=idServicio; this.nombre=nombre; this.descripcion=descripcion; this.precio=precio; this.duracionMinutos=duracionMinutos; this.estado=estado; this.createdAt=createdAt; this.updatedAt=updatedAt; }
    public UUID getIdServicio() { return idServicio; } public void setIdServicio(UUID value) { idServicio=value; }
    public String getNombre() { return nombre; } public void setNombre(String value) { nombre=value; }
    public String getDescripcion() { return descripcion; } public void setDescripcion(String value) { descripcion=value; }
    public BigDecimal getPrecio() { return precio; } public void setPrecio(BigDecimal value) { precio=value; }
    public Integer getDuracionMinutos() { return duracionMinutos; } public void setDuracionMinutos(Integer value) { duracionMinutos=value; }
    public Boolean getEstado() { return estado; } public void setEstado(Boolean value) { estado=value; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime value) { createdAt=value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime value) { updatedAt=value; }
}
