package com.lavarapido.backend_vehicular.shared.config;

import com.lavarapido.backend_vehicular.pagos.config.WompiConfigurationService;
import com.lavarapido.backend_vehicular.security.AdminIdentityPolicy;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Fail at startup instead of serving a partially configured production API. */
@Component
@Profile("prod")
public class ProductionReadiness {
    private final AdminIdentityPolicy administratorIds;
    private final WompiConfigurationService wompi;
    private final JdbcTemplate jdbc;

    @Value("${app.frontend.url}") private String frontendUrl;
    @Value("${app.cors.allowed-origins}") private String corsOrigins;
    @Value("${spring.datasource.password}") private String databasePassword;
    @Value("${spring.mail.username}") private String mailUsername;
    @Value("${spring.mail.password}") private String mailPassword;
    @Value("${spring.mail.host}") private String mailHost;

    public ProductionReadiness(AdminIdentityPolicy administratorIds, WompiConfigurationService wompi,
                               JdbcTemplate jdbc) {
        this.administratorIds = administratorIds;
        this.wompi = wompi;
        this.jdbc = jdbc;
    }

    @PostConstruct
    void validate() {
        if (!administratorIds.isConfigured()) {
            throw new IllegalStateException("Two verified administrator IDs are required in production");
        }
        if (blank(databasePassword) || blank(mailHost) || blank(mailUsername) || blank(mailPassword)) {
            throw new IllegalStateException("Production database and mail credentials are required");
        }
        try {
            if (!isHttpsOrigin(frontendUrl)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("A public HTTPS frontend URL is required", exception);
        }
        boolean validOrigins = !blank(corsOrigins) && Arrays.stream(corsOrigins.split(",", -1))
                .map(String::trim)
                .allMatch(this::isHttpsOrigin);
        if (!validOrigins || Arrays.stream(corsOrigins.split(","))
                .map(String::trim).noneMatch(frontendUrl::equals)) {
            throw new IllegalStateException("Explicit HTTPS CORS origins including the frontend URL are required");
        }
        wompi.validarWidget();
        wompi.validarConsulta();
        wompi.validarEventos();
        List<Boolean> administrators = jdbc.query("""
                SELECT ur.fk_user_id, u.status,
                       (SELECT count(*) FROM user_roles active_roles
                        WHERE active_roles.fk_user_id = ur.fk_user_id AND active_roles.status) AS active_roles
                FROM user_roles ur
                JOIN roles r ON r.role_id = ur.fk_role_id
                JOIN users u ON u.user_id = ur.fk_user_id
                WHERE ur.status AND r.role_name = 'ADMIN'
                """, (rs, row) -> administratorIds.isAuthorized((java.util.UUID) rs.getObject(1))
                        && rs.getBoolean(2) && rs.getInt(3) == 1);
        if (administrators.size() != 2 || administrators.stream().anyMatch(valid -> !valid)) {
            throw new IllegalStateException("Exactly two active, verified administrator accounts are required");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isHttpsOrigin(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getRawUserInfo() == null && uri.getRawQuery() == null
                    && uri.getRawFragment() == null && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                    && !value.contains("*");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
