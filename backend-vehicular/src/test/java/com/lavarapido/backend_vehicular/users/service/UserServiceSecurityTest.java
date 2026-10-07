package com.lavarapido.backend_vehicular.users.service;

import com.lavarapido.backend_vehicular.auth.dto.LoginDTO;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.security.JwtService;
import com.lavarapido.backend_vehicular.users.dto.UserRegistrationDTO;
import com.lavarapido.backend_vehicular.users.dto.UserRegistrationResponseDTO;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.enums.DocumentType;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceSecurityTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AccountAccessService access = mock(AccountAccessService.class);
    private final UserService service = new UserService(users, passwords, jwt, access);

    @Test
    void registrationReturnsOnlyPublicFields() {
        UUID id = UUID.randomUUID();
        when(passwords.encode(any())).thenReturn("bcrypt-hash-placeholder");
        when(users.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(id);
            return user;
        });
        UserRegistrationDTO input = new UserRegistrationDTO("new@example.test", "Ana", "Test",
                "3001234567", DocumentType.CC, "123456", "ValidPass123");

        UserRegistrationResponseDTO response = service.registerUser(input);

        assertEquals(id, response.userId());
        assertEquals("new@example.test", response.email());
        assertEquals(3, UserRegistrationResponseDTO.class.getRecordComponents().length);
        verify(passwords).encode("ValidPass123");
    }

    @Test
    void inactiveAccountCannotLoginEvenWithCorrectPassword() {
        User user = new User();
        user.setEmail("inactive@example.test");
        user.setStatus(false);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginDTO(user.getEmail(), "ValidPass123")));
        verifyNoInteractions(jwt);
    }

    @Test
    void invalidPasswordIsRejectedBeforeTokenCreation() {
        User user = new User();
        user.setEmail("active@example.test");
        user.setStatus(true);
        user.setPassword("stored-hash");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwords.matches("incorrect", "stored-hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginDTO(user.getEmail(), "incorrect")));
        verifyNoInteractions(jwt);
    }
}
