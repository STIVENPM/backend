package com.lavarapido.backend_vehicular.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordRulesTest {
    @Test
    void acceptsPasswordWithinBcryptByteLimit() {
        assertDoesNotThrow(() -> PasswordRules.requireValid("una-clave-larga"));
    }

    @Test
    void rejectsTooShortAndUtf8PasswordTruncatedByBcrypt() {
        assertThrows(IllegalArgumentException.class, () -> PasswordRules.requireValid("corta"));
        assertThrows(IllegalArgumentException.class, () -> PasswordRules.requireValid("é".repeat(40)));
    }
}
