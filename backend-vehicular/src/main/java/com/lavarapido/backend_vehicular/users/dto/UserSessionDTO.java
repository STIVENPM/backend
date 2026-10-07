package com.lavarapido.backend_vehicular.users.dto;

import java.util.UUID;

public record UserSessionDTO(UUID userId, String firstName, String email, String role) { }
