package com.lavarapido.backend_vehicular.users.service;

import com.lavarapido.backend_vehicular.auth.dto.LoginDTO;
import com.lavarapido.backend_vehicular.auth.dto.LoginResponseDTO;
import com.lavarapido.backend_vehicular.auth.service.PasswordRules;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.security.JwtService;
import com.lavarapido.backend_vehicular.users.dto.UserRegistrationDTO;
import com.lavarapido.backend_vehicular.users.dto.ChangePasswordRequestDTO;
import com.lavarapido.backend_vehicular.users.dto.UserProfileResponseDTO;
import com.lavarapido.backend_vehicular.users.dto.UserProfileUpdateDTO;
import com.lavarapido.backend_vehicular.users.dto.UserRegistrationResponseDTO;
import com.lavarapido.backend_vehicular.users.dto.UserSessionDTO;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.exception.CurrentPasswordInvalidException;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final AccountAccessService accountAccessService;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AccountAccessService accountAccessService) { this.userRepository=userRepository; this.passwordEncoder=passwordEncoder; this.jwtService=jwtService; this.accountAccessService=accountAccessService; }

    // Registra la cuenta en una transacción: un fallo revierte el guardado.
    @Transactional
    public UserRegistrationResponseDTO registerUser(UserRegistrationDTO dto) {

        // Rechaza una contraseña inválida antes de crear la entidad.
        PasswordRules.requireValid(dto.getPassword());

        // verifica disponibilidad del correo para evitar duplicados
        // antes de intentar guardar en la base de datos
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalStateException("El correo ya esta registrado");
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

        // Aquí la contraseña del DTO se convierte en hash BCrypt antes de persistir.
        user.setPassword(
            passwordEncoder.encode(dto.getPassword())
        );

        // Persiste la cuenta y devuelve sus datos públicos, sin contraseña.
        User saved = userRepository.save(user);
        return new UserRegistrationResponseDTO(saved.getUserId(), saved.getFirstName(), saved.getEmail());
    }

    // Compara la contraseña enviada con el hash guardado y emite el JWT.
public LoginResponseDTO login(LoginDTO dto) {

    User user = userRepository.findByEmail(dto.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));
    if (!Boolean.TRUE.equals(user.getStatus())) {
        throw new BadCredentialsException("Credenciales incorrectas");
    }

    // BCrypt compara el texto recibido con el hash de users.password.
    boolean valid = passwordEncoder.matches(
        dto.getPassword(),
        user.getPassword()
    );

    if (!valid) {
        throw new BadCredentialsException("Credenciales incorrectas");
    }

    // El rol se lee de las asignaciones activas, no de lo enviado por el cliente.
    String roleName = accountAccessService.currentRole(user);

    String token = jwtService.generateToken(user.getEmail());

    LoginResponseDTO.UserInfoDTO info =
            new LoginResponseDTO.UserInfoDTO(
                    user.getUserId().toString(),
                    user.getFirstName(),
                    user.getEmail(),
                    roleName
            );

    return new LoginResponseDTO(token, info);
}

    // Vuelve a consultar el usuario y su rol actual para /users/me.
    public UserSessionDTO getSession() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = accountAccessService.activeUser(email);
        return new UserSessionDTO(user.getUserId(), user.getFirstName(), user.getEmail(),
                accountAccessService.currentRole(user));
    }

    // Devuelve el perfil del usuario autenticado.
    public UserProfileResponseDTO getProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = accountAccessService.activeUser(email);
        return UserProfileResponseDTO.from(user);
    }

    // Actualiza solo los datos de perfil permitidos.
    @Transactional
    public UserProfileResponseDTO updateProfile(UserProfileUpdateDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = accountAccessService.activeUser(email);

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getProfilePicture() != null) {
            user.setProfilePicture(dto.getProfilePicture());
        }

        return UserProfileResponseDTO.from(userRepository.save(user));
    }

    // Actualiza la contraseña de la cuenta autenticada, sin aceptar un ID del cliente.
    @Transactional
    public void changePassword(ChangePasswordRequestDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            throw new BadCredentialsException("Invalid session");
        }

        User user = accountAccessService.activeUser(authentication.getName());
        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new CurrentPasswordInvalidException();
        }

        PasswordRules.requireValid(dto.newPassword());
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
    }
}
