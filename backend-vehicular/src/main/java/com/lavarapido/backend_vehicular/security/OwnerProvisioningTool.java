package com.lavarapido.backend_vehicular.security;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Interactive, offline-only provisioning for a brand-new seedless installation. */
public final class OwnerProvisioningTool {
    private OwnerProvisioningTool() { }

    private record Owner(UUID id, String email, String firstName, String lastName,
                         String phone, String documentType, String documentNumber, String hash) { }

    public static void main(String[] args) throws Exception {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Se requiere una terminal interactiva.");
        String url = requiredEnv("DB_JDBC_URL");
        String username = requiredEnv("DB_USERNAME");
        String databasePassword = requiredEnv("DB_PASSWORD");
        console.printf("Solo para una instalación NUEVA, vacía y verificada. No muestra contraseñas.%n");
        Owner first = readOwner(console, 1);
        Owner second = readOwner(console, 2);
        if (first.id().equals(second.id()) || first.email().equalsIgnoreCase(second.email())
                || first.documentNumber().equals(second.documentNumber())) {
            throw new IllegalArgumentException("Los dos perfiles deben ser distintos.");
        }
        try (Connection connection = DriverManager.getConnection(url, username, databasePassword)) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("LOCK TABLE users, user_roles IN SHARE ROW EXCLUSIVE MODE");
                    try (ResultSet rows = statement.executeQuery("SELECT count(*) FROM users")) {
                        rows.next();
                        if (rows.getLong(1) != 0) {
                            throw new IllegalStateException("La BD ya contiene usuarios; no se modifica ningún perfil.");
                        }
                    }
                }
                UUID adminRole = adminRole(connection);
                insertOwner(connection, first, adminRole);
                insertOwner(connection, second, adminRole);
                try (PreparedStatement check = connection.prepareStatement("""
                        SELECT count(*) FROM user_roles ur JOIN roles r ON r.role_id=ur.fk_role_id
                        WHERE ur.status AND r.role_name='ADMIN'
                        """); ResultSet rows = check.executeQuery()) {
                    rows.next();
                    if (rows.getLong(1) != 2) throw new IllegalStateException("El resultado no tiene dos ADMIN.");
                }
                connection.commit();
                console.printf("Se aprovisionaron exactamente dos perfiles ADMIN con los UUID indicados.%n");
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static Owner readOwner(Console console, int number) {
        console.printf("Perfil autorizado %d: confirme identidad y UUID fuera de banda antes de continuar.%n", number);
        UUID id = UUID.fromString(prompt(console, "UUID"));
        String email = prompt(console, "Correo").toLowerCase();
        String firstName = prompt(console, "Nombre");
        String lastName = prompt(console, "Apellido");
        String phone = prompt(console, "Celular colombiano");
        String documentType = prompt(console, "Tipo de documento (CC/TI/CE)");
        String documentNumber = prompt(console, "Número de documento");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || email.length() > 100
                || firstName.length() > 50 || lastName.length() > 50
                || !phone.matches("^3[0-9]{9}$")
                || !Arrays.asList("CC", "TI", "CE").contains(documentType)
                || documentNumber.isBlank() || documentNumber.length() > 12) {
            throw new IllegalArgumentException("Datos de perfil inválidos.");
        }
        char[] password = console.readPassword("Contraseña nueva: ");
        char[] confirmation = console.readPassword("Confirmar contraseña nueva: ");
        try {
            String plaintext = new String(password);
            if (!Arrays.equals(password, confirmation) || plaintext.length() < 8
                    || plaintext.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("La contraseña no cumple los requisitos.");
            }
            return new Owner(id, email, firstName, lastName, phone, documentType,
                    documentNumber, new BCryptPasswordEncoder(12).encode(plaintext));
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }

    private static String prompt(Console console, String label) {
        String value = console.readLine("%s: ", label);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " obligatorio.");
        return value.trim();
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Falta " + name);
        return value;
    }

    private static UUID adminRole(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT role_id FROM roles WHERE role_name='ADMIN'")) {
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new IllegalStateException("Falta el rol ADMIN.");
                return (UUID) rows.getObject(1);
            }
        }
    }

    private static void insertOwner(Connection connection, Owner owner, UUID adminRole) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO users (user_id,email,first_name,last_name,phone_number,
                                   document_type,document_number,password,status)
                VALUES (?,?,?,?,?,?,?,?,true)
                """)) {
            statement.setObject(1, owner.id());
            statement.setString(2, owner.email());
            statement.setString(3, owner.firstName());
            statement.setString(4, owner.lastName());
            statement.setString(5, owner.phone());
            statement.setString(6, owner.documentType());
            statement.setString(7, owner.documentNumber());
            statement.setString(8, owner.hash());
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO user_roles (fk_user_id,fk_role_id,status) VALUES (?,?,true)
                """)) {
            statement.setObject(1, owner.id());
            statement.setObject(2, adminRole);
            statement.executeUpdate();
        }
    }
}
