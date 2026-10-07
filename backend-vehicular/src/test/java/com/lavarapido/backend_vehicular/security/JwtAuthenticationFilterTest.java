package com.lavarapido.backend_vehicular.security;

import com.lavarapido.backend_vehicular.users.entity.User;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = mock(JwtService.class);
    private final AccountAccessService access = mock(AccountAccessService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, access);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void oldTokenCannotRetainRevokedAdministratorAuthority() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = new User();
        user.setUserId(UUID.randomUUID());
        when(jwt.isTokenValid("signed-token")).thenReturn(true);
        when(jwt.extractEmail("signed-token")).thenReturn("user@example.test");
        when(access.activeUser("user@example.test")).thenReturn(user);
        when(access.currentRole(user)).thenReturn("USER");
        FilterChain chain = (req, res) -> {
            var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
            assertEquals(1, authorities.size());
            assertEquals("ROLE_USER", authorities.iterator().next().getAuthority());
        };

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void inactiveAccountCannotUsePreviouslyIssuedToken() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwt.isTokenValid("signed-token")).thenReturn(true);
        when(jwt.extractEmail("signed-token")).thenReturn("user@example.test");
        when(access.activeUser("user@example.test"))
                .thenThrow(new BadCredentialsException("disabled"));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertFalse(response.getContentAsString().contains("disabled"));
        verifyNoInteractions(chain);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer signed-token");
        return request;
    }
}
