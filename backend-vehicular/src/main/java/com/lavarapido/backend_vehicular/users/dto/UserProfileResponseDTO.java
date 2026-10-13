package com.lavarapido.backend_vehicular.users.dto;

import java.util.UUID;

import com.lavarapido.backend_vehicular.users.entity.User;

/** Devuelve los datos públicos del perfil del usuario autenticado, sin contraseña. */
public record UserProfileResponseDTO(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String profilePicture
) {

    // Selecciona los campos del perfil que pueden salir en la respuesta.
    public static UserProfileResponseDTO from(User user) {
        return new UserProfileResponseDTO(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getProfilePicture()
        );
    }
}
