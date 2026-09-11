package com.lavarapido.backend_vehicular.servicios.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ServicioRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 300, message = "La descripcion no puede superar los 300 caracteres")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    @NotNull(message = "La duracion es obligatoria")
    @Min(value = 15, message = "La duracion minima es 15 minutos")
    @Max(value = 180, message = "La duracion maxima es 180 minutos")
    private Integer duracionMinutos;
    public ServicioRequestDTO() { }
    public ServicioRequestDTO(String nombre, String descripcion, BigDecimal precio, Integer duracionMinutos) { this.nombre=nombre; this.descripcion=descripcion; this.precio=precio; this.duracionMinutos=duracionMinutos; }
    public String getNombre() { return nombre; } public void setNombre(String value) { nombre=value; }
    public String getDescripcion() { return descripcion; } public void setDescripcion(String value) { descripcion=value; }
    public BigDecimal getPrecio() { return precio; } public void setPrecio(BigDecimal value) { precio=value; }
    public Integer getDuracionMinutos() { return duracionMinutos; } public void setDuracionMinutos(Integer value) { duracionMinutos=value; }
}
