package com.lavarapido.backend_vehicular.marcas.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/** Recibe el nombre de la marca que se quiere registrar en el catálogo. */
public record MarcaRequestDTO(

    @NotBlank(message = "El nombre de la marca es obligatorio")
    @Size(max = 30, message = "El nombre no puede superar los 30 caracteres")
    @Pattern(regexp = "[\\p{L}\\p{N} .&'-]+", message = "El nombre de la marca contiene caracteres no permitidos")
    String nombre

) {}