package com.lavarapido.backend_vehicular.auth.service;

import com.lavarapido.backend_vehicular.auth.repository.TokenRecuperacionRepository;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
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
    void shortPasswordIsRejectedBeforeTokenLookup() {
        assertThrows(IllegalArgumentException.class,
                () -> service.resetearContrasena("some-token", "short"));
        verifyNoInteractions(tokens, passwords);
    }
}
