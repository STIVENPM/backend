package com.lavarapido.backend_vehicular.users.dto;

import java.util.UUID;

/** Confirma el registro con el identificador, nombre y correo de la cuenta creada. */
public record UserRegistrationResponseDTO(UUID userId, String firstName, String email) { }
