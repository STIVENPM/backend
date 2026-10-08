package com.lavarapido.backend_vehicular.users.controller;

import com.lavarapido.backend_vehicular.auth.dto.LoginDTO;
import com.lavarapido.backend_vehicular.auth.dto.LoginResponseDTO;
import com.lavarapido.backend_vehicular.users.dto.UserProfileResponseDTO;
import com.lavarapido.backend_vehicular.users.dto.UserProfileUpdateDTO;
import com.lavarapido.backend_vehicular.users.dto.ChangePasswordRequestDTO;
import com.lavarapido.backend_vehicular.users.dto.UserRegistrationDTO;
import com.lavarapido.backend_vehicular.users.dto.UserRegistrationResponseDTO;
import com.lavarapido.backend_vehicular.users.dto.UserSessionDTO;
import com.lavarapido.backend_vehicular.users.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    //  REGISTRO
    // Recibe el registro validado y devuelve 201.
    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponseDTO> register(@Valid @RequestBody UserRegistrationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(dto));
    }

    //  LOGIN
    // Envía las credenciales al servicio y devuelve token y usuario.
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO dto) {
        return ResponseEntity.ok(userService.login(dto));
    }

    // Expone la identidad y el rol vigentes para el cliente.
    @GetMapping("/me")
    public ResponseEntity<UserSessionDTO> session() {
        return ResponseEntity.ok(userService.getSession());
    }

    // Devuelve el perfil del usuario autenticado.
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponseDTO> profile() {
        return ResponseEntity.ok(userService.getProfile());
    }

    // Valida el JSON y delega la actualización del perfil.
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponseDTO> updateProfile(@Valid @RequestBody UserProfileUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateProfile(dto));
    }

    // Cambia la contraseña de la cuenta identificada por el JWT.
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO dto) {
        userService.changePassword(dto);
        return ResponseEntity.noContent().build();
    }
}
