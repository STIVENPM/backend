package com.lavarapido.backend_vehicular.operadores.service;

import com.lavarapido.backend_vehicular.operadores.dto.OperadorEstadoRequestDTO;
import com.lavarapido.backend_vehicular.operadores.dto.OperadorRequestDTO;
import com.lavarapido.backend_vehicular.operadores.dto.OperadorResponseDTO;
import com.lavarapido.backend_vehicular.operadores.dto.OperadoresDesactivadosResponseDTO;
import com.lavarapido.backend_vehicular.operadores.entity.Operador;
import com.lavarapido.backend_vehicular.operadores.repository.OperadorRepository;
import com.lavarapido.backend_vehicular.roles.entity.Role;
import com.lavarapido.backend_vehicular.roles.repository.RoleRepository;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.entity.UserRole;
import com.lavarapido.backend_vehicular.users.entity.UserRoleId;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.users.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public OperadorResponseDTO crear(OperadorRequestDTO request) {
        User usuario = userRepository.findByEmail(request.email())
                .filter(user -> user.getDocumentNumber().equals(request.documentNumber()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con los datos proporcionados"));

        if (operadorRepository.existsByUsuario_UserId(usuario.getUserId())) {
            throw new IllegalStateException("El usuario ya está registrado como operador");
        }

        Role rolOperador = roleRepository.findByRoleName("OPERATOR")
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol OPERATOR no encontrado"));

        UserRoleId userRoleId = new UserRoleId(usuario.getUserId(), rolOperador.getRoleId());
        userRoleRepository.findActiveRoleByUserId(usuario.getUserId())
                .filter(userRole -> !userRole.getId().equals(userRoleId))
                .ifPresent(userRole -> {
                    userRole.setStatus(false);
                    userRole.setRevokedAt(LocalDateTime.now());
                    userRoleRepository.save(userRole);
                });
        UserRole userRole = userRoleRepository.findById(userRoleId).orElseGet(UserRole::new);
        userRole.setId(userRoleId);
        userRole.setUser(usuario);
        userRole.setRole(rolOperador);
        userRole.setStatus(true);
        userRole.setRevokedAt(null);
        userRoleRepository.save(userRole);

        Operador operador = Operador.builder()
                .usuario(usuario)
                .estado(true)
                .build();
        return mapearAResponse(operadorRepository.save(operador));
    }

    @Transactional(readOnly = true)
    public List<OperadorResponseDTO> listarTodos() {
        return operadorRepository.findAll().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional
    public OperadorResponseDTO cambiarEstado(UUID idOperador, OperadorEstadoRequestDTO request) {
        Operador operador = buscarPorId(idOperador);
        operador.setEstado(request.estado());
        return mapearAResponse(operadorRepository.save(operador));
    }

    @Transactional
    public OperadoresDesactivadosResponseDTO desactivarTodos() {
        int cantidadDesactivada = operadorRepository.desactivarTodosLosActivos();
        return new OperadoresDesactivadosResponseDTO(cantidadDesactivada);
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
