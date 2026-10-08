package com.lavarapido.backend_vehicular.auth.service;

import java.nio.charset.StandardCharsets;

public final class PasswordRules {
    private PasswordRules() { }

    // Exige 8 caracteres como mínimo y 72 bytes UTF-8 como máximo antes del hash.
    public static void requireValid(String password) {
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres y máximo 72 bytes UTF-8");
        }
    }
}
