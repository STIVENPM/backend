package com.lavarapido.backend_vehicular.users.service;

import com.lavarapido.backend_vehicular.auth.dto.LoginDTO;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.security.JwtService;
import com.lavarapido.backend_vehicular.users.dto.UserRegistrationDTO;
import com.lavarapido.backend_vehicular.users.dto.UserRegistrationResponseDTO;
import com.lavarapido.backend_vehicular.users.dto.ChangePasswordRequestDTO;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.enums.DocumentType;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

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

    @Test
    void changesPasswordForTheAuthenticatedAccountAndStoresOnlyEncodedValue() {
        User user = activeUser("authenticated@example.test", "old-hash");
        authenticate(user.getEmail());
        when(access.activeUser(user.getEmail())).thenReturn(user);
        when(passwords.matches("CurrentPass1", "old-hash")).thenReturn(true);
        when(passwords.encode("NewPass123")).thenReturn("new-bcrypt-hash");
        when(users.save(user)).thenReturn(user);

        service.changePassword(new ChangePasswordRequestDTO("CurrentPass1", "NewPass123"));

        verify(access).activeUser(user.getEmail());
        verify(passwords).matches("CurrentPass1", "old-hash");
        verify(passwords).encode("NewPass123");
        verify(users).save(user);
        assertEquals("new-bcrypt-hash", user.getPassword());
    }

    @Test
    void incorrectCurrentPasswordUsesBusinessErrorAndDoesNotSave() {
        User user = activeUser("authenticated@example.test", "old-hash");
        authenticate(user.getEmail());
        when(access.activeUser(user.getEmail())).thenReturn(user);
        when(passwords.matches("WrongPass1", "old-hash")).thenReturn(false);

        assertThrows(com.lavarapido.backend_vehicular.users.exception.CurrentPasswordInvalidException.class,
                () -> service.changePassword(new ChangePasswordRequestDTO("WrongPass1", "NewPass123")));

        verify(users, never()).save(any());
        verify(passwords, never()).encode(any());
    }

    @Test
    void rejectsInvalidNewPasswordAfterVerifyingCurrentPassword() {
        User user = activeUser("authenticated@example.test", "old-hash");
        authenticate(user.getEmail());
        when(access.activeUser(user.getEmail())).thenReturn(user);
        when(passwords.matches("CurrentPass1", "old-hash")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.changePassword(new ChangePasswordRequestDTO("CurrentPass1", "short")));

        verify(users, never()).save(any());
        verify(passwords, never()).encode(any());
    }

    @Test
    void rejectsWhitespaceOnlyNewPassword() {
        User user = activeUser("authenticated@example.test", "old-hash");
        authenticate(user.getEmail());
        when(access.activeUser(user.getEmail())).thenReturn(user);
        when(passwords.matches("CurrentPass1", "old-hash")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.changePassword(new ChangePasswordRequestDTO("CurrentPass1", "        ")));

        verify(users, never()).save(any());
    }

    @Test
    void requiresAnAuthenticatedPrincipal() {
        SecurityContextHolder.clearContext();

        assertThrows(BadCredentialsException.class,
                () -> service.changePassword(new ChangePasswordRequestDTO("CurrentPass1", "NewPass123")));

        verifyNoInteractions(access, passwords, users);
    }

    @Test
    void rejectsInactiveAuthenticatedAccount() {
        authenticate("inactive@example.test");
        when(access.activeUser("inactive@example.test"))
                .thenThrow(new BadCredentialsException("Invalid session"));

        assertThrows(BadCredentialsException.class,
                () -> service.changePassword(new ChangePasswordRequestDTO("CurrentPass1", "NewPass123")));

        verifyNoInteractions(passwords, users);
    }

    @Test
    void requestHasNoExternalUserIdAndUsesOnlyJwtIdentity() {
        User authenticatedUser = activeUser("jwt-user@example.test", "old-hash");
        User otherUser = activeUser("other-user@example.test", "other-hash");
        authenticate(authenticatedUser.getEmail());
        when(access.activeUser(authenticatedUser.getEmail())).thenReturn(authenticatedUser);
        when(passwords.matches("CurrentPass1", "old-hash")).thenReturn(true);
        when(passwords.encode("NewPass123")).thenReturn("encoded-for-jwt-user");
        when(users.save(authenticatedUser)).thenReturn(authenticatedUser);

        service.changePassword(new ChangePasswordRequestDTO("CurrentPass1", "NewPass123"));

        assertEquals(2, ChangePasswordRequestDTO.class.getRecordComponents().length);
        verify(access).activeUser("jwt-user@example.test");
        verify(access, never()).activeUser(otherUser.getEmail());
        verify(users).save(authenticatedUser);
        verify(users, never()).save(otherUser);
    }

    private static User activeUser(String email, String passwordHash) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordHash);
        user.setStatus(true);
        return user;
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "unused", java.util.List.of()));
    }
}
