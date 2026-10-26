package com.lavarapido.backend_vehicular.users.dto;

import java.util.UUID;

/** Devuelve la identidad y el rol vigentes de la sesión al consultar /api/users/me. */
public record UserSessionDTO(UUID userId, String firstName, String email, String role) { }
