package com.lavarapido.backend_vehicular.users.controller;

import com.lavarapido.backend_vehicular.auth.dto.LoginDTO;
import com.lavarapido.backend_vehicular.auth.dto.LoginResponseDTO;
import com.lavarapido.backend_vehicular.users.dto.UserProfileResponseDTO;
import com.lavarapido.backend_vehicular.users.dto.UserProfileUpdateDTO;
import com.lavarapido.backend_vehicular.users.dto.UserRegistrationDTO;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 🔥 REGISTRO
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationDTO dto) {

        try {
            User user = userService.registerUser(dto);
            return ResponseEntity.ok(user);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔐 LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO dto) {

        try {
            LoginResponseDTO response = userService.login(dto);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile() {
        try {
            UserProfileResponseDTO response = userService.getProfile();
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserProfileUpdateDTO dto) {
        try {
            UserProfileResponseDTO response = userService.updateProfile(dto);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
