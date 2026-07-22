package com.lavarapido.backend_vehicular.marcas.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarcaRequestDTO(

    @NotBlank(message = "El nombre de la marca es obligatorio")
    @Size(max = 30, message = "El nombre no puede superar los 30 caracteres")
    String nombre

) {}