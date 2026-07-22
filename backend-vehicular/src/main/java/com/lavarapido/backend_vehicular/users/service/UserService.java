package com.lavarapido.backend_vehicular.users.service;

import com.lavarapido.backend_vehicular.auth.dto.LoginDTO;
import com.lavarapido.backend_vehicular.auth.dto.LoginResponseDTO;
import com.lavarapido.backend_vehicular.security.JwtService;
import com.lavarapido.backend_vehicular.users.dto.UserRegistrationDTO;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.users.repository.UserRoleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRoleRepository userRoleRepository;

    // 🔥 REGISTRO DE USUARIO
    // @Transactional garantiza que si ocurre un error durante el proceso,
    // toda la operacion se revierte automaticamente (rollback)
    // evitando registros incompletos o inconsistentes en base de datos
    @Transactional
    public User registerUser(UserRegistrationDTO dto) {

        // verifica disponibilidad del correo para evitar duplicados
        // antes de intentar guardar en la base de datos
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("el correo ya esta registrado");
        }

        // se crea una nueva instancia de la entidad User
        // que sera persistida en la tabla users
        User user = new User();

        // transferencia manual de datos desde el DTO hacia la entidad
        // este proceso se conoce como mapeo manual
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setDocumentType(dto.getDocumentType());
        user.setDocumentNumber(dto.getDocumentNumber());

        // encriptacion de la contrasena usando BCrypt
        // nunca se debe guardar la contrasena en texto plano
        // el resultado encaja con CHAR(60) en PostgreSQL
        user.setPassword(
            passwordEncoder.encode(dto.getPassword())
        );

        // guarda el usuario en la base de datos
        // Hibernate genera el UUID automaticamente y retorna
        // el objeto persistido con sus datos completos
        return userRepository.save(user);
    }

// 🔐 LOGIN DE USUARIO
public LoginResponseDTO login(LoginDTO dto) {

    var userOpt = userRepository.findByEmail(dto.getEmail());

    if (userOpt.isEmpty()) {
        throw new RuntimeException("Usuario no encontrado");
    }

    User user = userOpt.get();

    boolean valid = passwordEncoder.matches(
        dto.getPassword(),
        user.getPassword()
    );

    if (!valid) {
        throw new RuntimeException("Contrasena incorrecta");
    }

    var userRoleOpt = userRoleRepository
            .findActiveRoleByUserId(user.getUserId());

    String roleName = userRoleOpt
            .map(ur -> ur.getRole().getRoleName())
            .orElse("USER");

    // ← MODIFICADO: ahora se pasa el rol como segundo argumento
    String token = jwtService.generateToken(user.getEmail(), roleName);

    LoginResponseDTO.UserInfoDTO info =
            new LoginResponseDTO.UserInfoDTO(
                    user.getUserId().toString(),
                    user.getFirstName(),
                    user.getEmail(),
                    roleName
            );

    return new LoginResponseDTO(token, info);
}
}
