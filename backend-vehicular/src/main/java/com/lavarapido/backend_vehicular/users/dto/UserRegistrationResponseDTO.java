package com.lavarapido.backend_vehicular.users.dto;

import java.util.UUID;

public record UserRegistrationResponseDTO(UUID userId, String firstName, String email) { }
