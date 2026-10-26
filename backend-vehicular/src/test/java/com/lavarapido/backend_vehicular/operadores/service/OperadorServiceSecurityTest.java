package com.lavarapido.backend_vehicular.operadores.service;

import com.lavarapido.backend_vehicular.operadores.dto.OperadorEstadoRequestDTO;
import com.lavarapido.backend_vehicular.operadores.dto.OperadorRequestDTO;
import com.lavarapido.backend_vehicular.operadores.entity.Operador;
import com.lavarapido.backend_vehicular.operadores.repository.OperadorRepository;
import com.lavarapido.backend_vehicular.roles.entity.Role;
import com.lavarapido.backend_vehicular.roles.repository.RoleRepository;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.security.AdminIdentityPolicy;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.entity.UserRole;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.users.repository.UserRoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OperadorServiceSecurityTest {
    private final OperadorRepository operators = mock(OperadorRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final UserRoleRepository assignments = mock(UserRoleRepository.class);
    private final AccountAccessService access = mock(AccountAccessService.class);
    private final AdminIdentityPolicy adminIds = mock(AdminIdentityPolicy.class);
    private final OperadorService service = new OperadorService(operators, users, roles, assignments, access, adminIds);
    private final UUID targetId = UUID.fromString("00000000-0000-0000-0000-000000000021");

    @Test
    void userOrOperatorCannotAssignRolesThroughService() {
        OperadorRequestDTO request = new OperadorRequestDTO("target@example.test", "123");
        doThrow(new AccessDeniedException("denied")).when(access).requireAdministrator();
        assertThrows(AccessDeniedException.class, () -> service.crear(request));
        assertThrows(AccessDeniedException.class, () -> service.desactivarTodos());
        verifyNoInteractions(users, roles, assignments, operators);
    }

    @Test
    void verifiedAdminCanAssignOperatorButNeverAdmin() {
        User target = target();
        UserRole previous = assignment(target, "USER");
        when(users.findByEmail(target.getEmail())).thenReturn(Optional.of(target));
        when(access.currentRole(target)).thenReturn("USER");
        when(assignments.findAllByUser_UserIdAndStatusTrue(targetId)).thenReturn(List.of(previous));
        when(roles.findByRoleName("OPERATOR")).thenReturn(Optional.of(new Role(UUID.randomUUID(), "OPERATOR", null)));
        when(operators.save(any())).thenAnswer(invocation -> {
            Operador operator = invocation.getArgument(0);
            operator.setIdOperador(UUID.randomUUID());
            return operator;
        });

        assertNotNull(service.crear(new OperadorRequestDTO(target.getEmail(), target.getDocumentNumber())));
        assertFalse(previous.getStatus());
        verify(roles).findByRoleName("OPERATOR");
        verify(roles, never()).findByRoleName("ADMIN");
        verify(assignments, atLeastOnce()).save(any());
    }

    @Test
    void deactivationRevokesOperatorAndRestoresUser() {
        User target = target();
        Operador operator = Operador.builder().idOperador(UUID.randomUUID()).usuario(target).estado(true).build();
        UserRole previous = assignment(target, "OPERATOR");
        when(operators.findById(operator.getIdOperador())).thenReturn(Optional.of(operator));
        when(assignments.findAllByUser_UserIdAndStatusTrue(targetId)).thenReturn(List.of(previous));
        when(roles.findByRoleName("USER")).thenReturn(Optional.of(new Role(UUID.randomUUID(), "USER", null)));
        when(operators.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.cambiarEstado(operator.getIdOperador(), new OperadorEstadoRequestDTO(false));

        assertFalse(previous.getStatus());
        assertFalse(operator.getEstado());
        verify(roles, never()).findByRoleName("ADMIN");
    }

    @Test
    void existingAdministratorIdentityCannotBecomeOperator() {
        User target = target();
        when(users.findByEmail(target.getEmail())).thenReturn(Optional.of(target));
        when(adminIds.isAuthorized(targetId)).thenReturn(true);
        assertThrows(AccessDeniedException.class,
                () -> service.crear(new OperadorRequestDTO(target.getEmail(), target.getDocumentNumber())));
        verify(operators, never()).save(any());
    }

    private User target() {
        User user = new User();
        user.setUserId(targetId);
        user.setEmail("target@example.test");
        user.setDocumentNumber("123");
        user.setStatus(true);
        return user;
    }

    private UserRole assignment(User user, String name) {
        UserRole assignment = new UserRole();
        assignment.setUser(user);
        assignment.setRole(new Role(UUID.randomUUID(), name, null));
        assignment.setStatus(true);
        return assignment;
    }
}
