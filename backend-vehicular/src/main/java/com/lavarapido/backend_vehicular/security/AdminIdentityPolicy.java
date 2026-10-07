package com.lavarapido.backend_vehicular.security;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** No endpoint can add an administrator. These IDs must be verified out of band. */
@Component
public class AdminIdentityPolicy {
    private final Set<UUID> authorizedIds;

    public AdminIdentityPolicy(@Value("${security.admin-user-ids:}") String configuredIds) {
        if (configuredIds == null || configuredIds.isBlank()) {
            authorizedIds = Set.of();
            return;
        }
        try {
            String[] values = configuredIds.split(",", -1);
            authorizedIds = Arrays.stream(values)
                    .map(String::trim)
                    .map(UUID::fromString)
                    .collect(Collectors.toUnmodifiableSet());
            if (values.length != 2 || authorizedIds.size() != 2) {
                throw new IllegalArgumentException("Exactly two distinct administrator IDs are required");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid administrator identity configuration", exception);
        }
    }

    public boolean isAuthorized(UUID userId) {
        return userId != null && authorizedIds.contains(userId);
    }

    public boolean isConfigured() {
        return authorizedIds.size() == 2;
    }
}
