package com.lavarapido.backend_vehicular.auth.service;

import com.lavarapido.backend_vehicular.auth.entity.TokenRecuperacion;
import com.lavarapido.backend_vehicular.auth.exception.EmailDeliveryException;
import com.lavarapido.backend_vehicular.auth.repository.TokenRecuperacionRepository;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordResetSecurityTest {
    private final UserRepository users = mock(UserRepository.class);
    private final TokenRecuperacionRepository tokens = mock(TokenRecuperacionRepository.class);
    private final EmailService email = mock(EmailService.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final PasswordResetService service = new PasswordResetService(users, tokens, email, passwords);

    @Test
    void unknownEmailDoesNotExposeItsExistence() {
        when(users.findByEmail("missing@example.test")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.solicitarRecuperacion("missing@example.test", "127.0.0.1"));
        verifyNoInteractions(tokens, email);
    }

    @Test
    void validRequestStoresOnlyTokenHashAndInvalidatesPreviousTokens() {
        User user = activeUser();
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var stored = org.mockito.ArgumentCaptor.forClass(TokenRecuperacion.class);
        var sentToken = org.mockito.ArgumentCaptor.forClass(String.class);

        service.solicitarRecuperacion(user.getEmail(), "127.0.0.1");

        verify(tokens).invalidarTokensActivosPorUsuario(user.getUserId());
        verify(tokens).saveAndFlush(stored.capture());
        verify(email).enviarCorreoRecuperacion(eq(user.getEmail()), sentToken.capture());
        assertEquals(64, stored.getValue().getTokenHash().length());
        assertNotEquals(sentToken.getValue(), stored.getValue().getTokenHash());
        assertFalse(stored.getValue().getUsado());
    }

    @Test
    void smtpFailurePreservesItsCauseForSafeDiagnostics() {
        User user = activeUser();
        MailAuthenticationException mailFailure = new MailAuthenticationException("authentication failed");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        doThrow(mailFailure).when(email).enviarCorreoRecuperacion(anyString(), anyString());

        EmailDeliveryException exception = assertThrows(EmailDeliveryException.class,
                () -> service.solicitarRecuperacion(user.getEmail(), "127.0.0.1"));

        assertSame(mailFailure, exception.getCause());
        assertEquals("authentication", exception.failureCategory());
        verify(tokens).saveAndFlush(any(TokenRecuperacion.class));
    }

    @Test
    void shortPasswordIsRejectedBeforeTokenLookup() {
        assertThrows(IllegalArgumentException.class,
                () -> service.resetearContrasena("some-token", "short"));
        verifyNoInteractions(tokens, passwords);
    }

    @Test
    void validTokenResetsEncodedPasswordAndConsumesToken() {
        User user = activeUser();
        TokenRecuperacion token = validToken(user);
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(passwords.encode("NuevaClave1")).thenReturn("encoded-password");

        service.resetearContrasena("plain-token", "NuevaClave1");

        assertEquals("encoded-password", user.getPassword());
        assertTrue(token.getUsado());
        verify(users).save(user);
        verify(tokens).save(token);
    }

    @Test
    void expiredAndUsedTokensAreRejectedWithoutChangingPassword() {
        User user = activeUser();
        TokenRecuperacion expired = validToken(user);
        expired.setExpiracionAt(LocalDateTime.now().minusMinutes(1));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThrows(IllegalArgumentException.class,
                () -> service.resetearContrasena("expired-token", "NuevaClave1"));
        verifyNoInteractions(passwords);
        verify(users, never()).save(any());

        reset(tokens, passwords, users);
        TokenRecuperacion used = validToken(user);
        used.setUsado(true);
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(used));
        assertThrows(IllegalArgumentException.class,
                () -> service.resetearContrasena("used-token", "NuevaClave1"));
        verifyNoInteractions(passwords);
    }

    @Test
    void nonexistentTokenIsRejected() {
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.resetearContrasena("unknown-token", "NuevaClave1"));
        verifyNoInteractions(passwords);
    }

    private User activeUser() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail("user@example.test");
        user.setStatus(true);
        return user;
    }

    private TokenRecuperacion validToken(User user) {
        TokenRecuperacion token = new TokenRecuperacion();
        token.setUsuario(user);
        token.setUsado(false);
        token.setExpiracionAt(LocalDateTime.now().plusMinutes(30));
        return token;
    }
}
