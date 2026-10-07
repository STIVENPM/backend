package com.lavarapido.backend_vehicular.operadores.service;

import com.lavarapido.backend_vehicular.operadores.dto.OperadorEstadoRequestDTO;
import com.lavarapido.backend_vehicular.operadores.dto.OperadorRequestDTO;
import com.lavarapido.backend_vehicular.operadores.dto.OperadorResponseDTO;
import com.lavarapido.backend_vehicular.operadores.dto.OperadoresDesactivadosResponseDTO;
import com.lavarapido.backend_vehicular.operadores.entity.Operador;
import com.lavarapido.backend_vehicular.operadores.repository.OperadorRepository;
import com.lavarapido.backend_vehicular.roles.entity.Role;
import com.lavarapido.backend_vehicular.roles.repository.RoleRepository;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.security.AdminIdentityPolicy;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.entity.UserRole;
import com.lavarapido.backend_vehicular.users.entity.UserRoleId;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.users.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperadorService {

    private final OperadorRepository operadorRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AccountAccessService accountAccessService;
    private final AdminIdentityPolicy adminIdentityPolicy;

    @Transactional
    public OperadorResponseDTO crear(OperadorRequestDTO request) {
        accountAccessService.requireAdministrator();
        User usuario = userRepository.findByEmail(request.email())
                .filter(user -> user.getDocumentNumber().equals(request.documentNumber()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con los datos proporcionados"));

        if (!Boolean.TRUE.equals(usuario.getStatus()) || adminIdentityPolicy.isAuthorized(usuario.getUserId())
                || !"USER".equals(accountAccessService.currentRole(usuario))) {
            throw new AccessDeniedException("El usuario no puede recibir el rol de operador");
        }

        if (operadorRepository.existsByUsuario_UserId(usuario.getUserId())) {
            throw new IllegalStateException("El usuario ya está registrado como operador");
        }

        setRole(usuario, "OPERATOR");

        Operador operador = Operador.builder()
                .usuario(usuario)
                .estado(true)
                .build();
        return mapearAResponse(operadorRepository.save(operador));
    }

    @Transactional(readOnly = true)
    public List<OperadorResponseDTO> listarTodos() {
        accountAccessService.requireAdministrator();
        return operadorRepository.findAll().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional
    public OperadorResponseDTO cambiarEstado(UUID idOperador, OperadorEstadoRequestDTO request) {
        accountAccessService.requireAdministrator();
        Operador operador = buscarPorId(idOperador);
        User usuario = operador.getUsuario();
        if (Boolean.TRUE.equals(request.estado())) {
            if (!Boolean.TRUE.equals(usuario.getStatus()) || adminIdentityPolicy.isAuthorized(usuario.getUserId())) {
                throw new AccessDeniedException("El usuario no puede recibir el rol de operador");
            }
            String currentRole = accountAccessService.currentRole(usuario);
            if (!"USER".equals(currentRole) && !"OPERATOR".equals(currentRole)) {
                throw new AccessDeniedException("El usuario no puede recibir el rol de operador");
            }
            setRole(usuario, "OPERATOR");
        } else {
            setRole(usuario, "USER");
        }
        operador.setEstado(request.estado());
        return mapearAResponse(operadorRepository.save(operador));
    }

    @Transactional
    public OperadoresDesactivadosResponseDTO desactivarTodos() {
        accountAccessService.requireAdministrator();
        int count = 0;
        for (Operador operador : operadorRepository.findAll()) {
            if (Boolean.TRUE.equals(operador.getEstado())) {
                if (adminIdentityPolicy.isAuthorized(operador.getUsuario().getUserId())) {
                    throw new AccessDeniedException("Una cuenta administradora no puede ser operador");
                }
                setRole(operador.getUsuario(), "USER");
                operador.setEstado(false);
                operadorRepository.save(operador);
                count++;
            }
        }
        return new OperadoresDesactivadosResponseDTO(count);
    }

    private void setRole(User usuario, String roleName) {
        if (!"USER".equals(roleName) && !"OPERATOR".equals(roleName)) {
            throw new AccessDeniedException("El rol solicitado no esta permitido");
        }
        if (adminIdentityPolicy.isAuthorized(usuario.getUserId())) {
            throw new AccessDeniedException("No se puede modificar una cuenta administradora");
        }
        List<UserRole> active = userRoleRepository.findAllByUser_UserIdAndStatusTrue(usuario.getUserId());
        if (active.stream().anyMatch(ur -> "ADMIN".equals(ur.getRole().getRoleName()))) {
            throw new AccessDeniedException("No se puede modificar una cuenta administradora");
        }
        for (UserRole current : active) {
            if (!roleName.equals(current.getRole().getRoleName())) {
                current.setStatus(false);
                current.setRevokedAt(LocalDateTime.now());
                userRoleRepository.save(current);
            }
        }
        if (active.stream().anyMatch(ur -> roleName.equals(ur.getRole().getRoleName()))) return;

        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException("Rol no configurado"));
        UserRoleId id = new UserRoleId(usuario.getUserId(), role.getRoleId());
        UserRole assignment = userRoleRepository.findById(id).orElseGet(UserRole::new);
        assignment.setId(id);
        assignment.setUser(usuario);
        assignment.setRole(role);
        assignment.setStatus(true);
        assignment.setRevokedAt(null);
        userRoleRepository.save(assignment);
    }

    private Operador buscarPorId(UUID idOperador) {
        return operadorRepository.findById(idOperador)
                .orElseThrow(() -> new RecursoNoEncontradoException("Operador no encontrado"));
    }

    private OperadorResponseDTO mapearAResponse(Operador operador) {
        User usuario = operador.getUsuario();
        String firstName = Optional.ofNullable(usuario.getFirstName()).orElse("");
        String lastName = Optional.ofNullable(usuario.getLastName()).orElse("");
        return new OperadorResponseDTO(operador.getIdOperador(), usuario.getUserId(), firstName, lastName,
                usuario.getEmail(), operador.getEstado(), operador.getCreatedAt(), operador.getUpdatedAt());
    }
}
