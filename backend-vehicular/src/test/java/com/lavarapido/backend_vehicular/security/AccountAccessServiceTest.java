package com.lavarapido.backend_vehicular.security;

import com.lavarapido.backend_vehicular.operadores.entity.Operador;
import com.lavarapido.backend_vehicular.operadores.repository.OperadorRepository;
import com.lavarapido.backend_vehicular.roles.entity.Role;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.entity.UserRole;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.users.repository.UserRoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountAccessServiceTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID OTHER_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");
    private final UserRepository users = mock(UserRepository.class);
    private final UserRoleRepository roles = mock(UserRoleRepository.class);
    private final OperadorRepository operators = mock(OperadorRepository.class);
    private final AdminIdentityPolicy policy = new AdminIdentityPolicy(ADMIN_ID + "," + OTHER_ADMIN_ID);
    private final AccountAccessService access = new AccountAccessService(users, roles, operators, policy);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userAndOperatorCannotPerformAdministratorOperation() {
        User user = user(USER_ID, true);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null));

        when(roles.findAllByUser_UserIdAndStatusTrue(USER_ID)).thenReturn(List.of(role(user, "USER")));
        assertThrows(AccessDeniedException.class, access::requireAdministrator);

        when(roles.findAllByUser_UserIdAndStatusTrue(USER_ID)).thenReturn(List.of(role(user, "OPERATOR")));
        when(operators.findByUsuario_UserId(USER_ID)).thenReturn(Optional.of(
                Operador.builder().usuario(user).estado(true).build()));
        assertThrows(AccessDeniedException.class, access::requireAdministrator);
    }

    @Test
    void revokedAdministratorRoleLosesAccessImmediately() {
        User admin = user(ADMIN_ID, true);
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin.getEmail(), null));
        when(roles.findAllByUser_UserIdAndStatusTrue(ADMIN_ID))
                .thenReturn(List.of(role(admin, "ADMIN")), List.of(role(admin, "USER")));

        assertEquals(ADMIN_ID, access.requireAdministrator().getUserId());
        assertThrows(AccessDeniedException.class, access::requireAdministrator);
    }

    @Test
    void administratorDatabaseRoleOutsideVerifiedIdsIsRejected() {
        User stranger = user(USER_ID, true);
        when(roles.findAllByUser_UserIdAndStatusTrue(USER_ID)).thenReturn(List.of(role(stranger, "ADMIN")));
        assertThrows(BadCredentialsException.class, () -> access.currentRole(stranger));
    }

    @Test
    void deactivatedAccountAndOperatorLoseTheirAuthority() {
        User user = user(USER_ID, true);
        when(roles.findAllByUser_UserIdAndStatusTrue(USER_ID)).thenReturn(List.of(role(user, "OPERATOR")));
        when(operators.findByUsuario_UserId(USER_ID)).thenReturn(Optional.of(
                Operador.builder().usuario(user).estado(false).build()));
        assertEquals("USER", access.currentRole(user));

        user.setStatus(false);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        assertThrows(BadCredentialsException.class, () -> access.activeUser(user.getEmail()));
    }

    private User user(UUID id, boolean active) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(id + "@example.test");
        user.setStatus(active);
        return user;
    }

    private UserRole role(User user, String name) {
        UserRole assignment = new UserRole();
        assignment.setUser(user);
        assignment.setRole(new Role(UUID.randomUUID(), name, null));
        assignment.setStatus(true);
        return assignment;
    }
}
