package com.lavarapido.backend_vehicular.security;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Ningún endpoint crea administradores; sus UUID se autorizan fuera de la API. */
@Component
public class AdminIdentityPolicy {
    private final Set<UUID> authorizedIds;

    public AdminIdentityPolicy(@Value("${security.admin-user-ids:}") String configuredIds) {
        // Sin lista configurada ningun UUID puede superar la comprobacion ADMIN.
        if (configuredIds == null || configuredIds.isBlank()) {
            authorizedIds = Set.of();
            return;
        }
        try {
            // Convierte la lista de configuracion a UUID y elimina duplicados.
            String[] values = configuredIds.split(",", -1);
            authorizedIds = Arrays.stream(values)
                    .map(String::trim)
                    .map(UUID::fromString)
                    .collect(Collectors.toUnmodifiableSet());
            if (values.length != 2 || authorizedIds.size() != 2) {
                // El arranque falla si no hay exactamente dos identidades distintas.
                throw new IllegalArgumentException("Exactly two distinct administrator IDs are required");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid administrator identity configuration", exception);
        }
    }

    public boolean isAuthorized(UUID userId) {
        // AccountAccessService usa esta comprobacion al resolver el rol vigente.
        return userId != null && authorizedIds.contains(userId);
    }

    public boolean isConfigured() {
        return authorizedIds.size() == 2;
    }
}
