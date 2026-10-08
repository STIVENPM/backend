package com.lavarapido.backend_vehicular.security;

import com.lavarapido.backend_vehicular.operadores.repository.OperadorRepository;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.entity.UserRole;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.users.repository.UserRoleRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AccountAccessService {
    private final UserRepository users;
    private final UserRoleRepository roles;
    private final OperadorRepository operadores;
    private final AdminIdentityPolicy administratorIds;

    public AccountAccessService(UserRepository users, UserRoleRepository roles,
                                OperadorRepository operadores, AdminIdentityPolicy administratorIds) {
        this.users = users;
        this.roles = roles;
        this.operadores = operadores;
        this.administratorIds = administratorIds;
    }

    // Relee la cuenta para invalidar sesiones de usuarios desactivados.
    public User activeUser(String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid session"));
        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new BadCredentialsException("Invalid session");
        }
        return user;
    }

    // Consulta roles activos; rechaza múltiples roles o un ADMIN no autorizado.
    public String currentRole(User user) {
        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new BadCredentialsException("Invalid session");
        }
        List<UserRole> active = roles.findAllByUser_UserIdAndStatusTrue(user.getUserId());
        if (active.size() > 1) {
            throw new BadCredentialsException("Invalid role state");
        }
        if (active.isEmpty()) return "USER";

        String role = active.get(0).getRole().getRoleName();
        return switch (role) {
            case "ADMIN" -> {
                if (!administratorIds.isAuthorized(user.getUserId())) {
                    throw new BadCredentialsException("Invalid role state");
                }
                yield "ADMIN";
            }
            // Sin una fila de operador activa, la cuenta conserva permisos USER.
            case "OPERATOR" -> operadores.findByUsuario_UserId(user.getUserId())
                    .filter(operador -> Boolean.TRUE.equals(operador.getEstado()))
                    .map(operador -> "OPERATOR")
                    .orElse("USER");
            case "USER" -> "USER";
            default -> throw new BadCredentialsException("Invalid role state");
        };
    }

    // Exige el rol ADMIN vigente para operaciones sensibles del servicio.
    public User requireAdministrator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Administrator required");
        }
        try {
            User user = activeUser(authentication.getName());
            if ("ADMIN".equals(currentRole(user))) return user;
        } catch (BadCredentialsException exception) {
            // A revoked or disabled account has no administrator authority.
        }
        throw new AccessDeniedException("Administrator required");
    }
}
