package com.lavarapido.backend_vehicular.marcas.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Recibe el nombre de la marca que se quiere registrar en el catálogo. */
public record MarcaRequestDTO(

    @NotBlank(message = "El nombre de la marca es obligatorio")
    @Size(max = 30, message = "El nombre no puede superar los 30 caracteres")
    String nombre

) {}