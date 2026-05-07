package com.lavarapido.backend_vehicular.controllers;

import com.lavarapido.backend_vehicular.dto.LoginDTO;
import com.lavarapido.backend_vehicular.dto.LoginResponseDTO;
import com.lavarapido.backend_vehicular.dto.UserRegistrationDTO;
import com.lavarapido.backend_vehicular.entities.User;
import com.lavarapido.backend_vehicular.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

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
    public String profile() {

        var auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String email = auth.getName();

        return "Authenticated user: " + email;
    }
}