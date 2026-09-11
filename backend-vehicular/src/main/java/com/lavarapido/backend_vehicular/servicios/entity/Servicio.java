package com.lavarapido.backend_vehicular.servicios.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "servicios")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_servicio")
    private UUID idServicio;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 10, scale = 0)
    private BigDecimal precio;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Servicio() { }
    public Servicio(UUID idServicio, String nombre, String descripcion, BigDecimal precio, Integer duracionMinutos, Boolean estado, LocalDateTime createdAt, LocalDateTime updatedAt) { this.idServicio=idServicio; this.nombre=nombre; this.descripcion=descripcion; this.precio=precio; this.duracionMinutos=duracionMinutos; this.estado=estado; this.createdAt=createdAt; this.updatedAt=updatedAt; }
    public UUID getIdServicio() { return idServicio; } public void setIdServicio(UUID value) { idServicio=value; }
    public String getNombre() { return nombre; } public void setNombre(String value) { nombre=value; }
    public String getDescripcion() { return descripcion; } public void setDescripcion(String value) { descripcion=value; }
    public BigDecimal getPrecio() { return precio; } public void setPrecio(BigDecimal value) { precio=value; }
    public Integer getDuracionMinutos() { return duracionMinutos; } public void setDuracionMinutos(Integer value) { duracionMinutos=value; }
    public Boolean getEstado() { return estado; } public void setEstado(Boolean value) { estado=value; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime value) { createdAt=value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime value) { updatedAt=value; }
}
