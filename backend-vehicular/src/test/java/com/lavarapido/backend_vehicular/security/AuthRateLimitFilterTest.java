package com.lavarapido.backend_vehicular.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthRateLimitFilterTest {
    @Test
    void blocksRepeatedFailedLoginAttemptsByClientAddress() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
        FilterChain failedLogin = (request, response) -> ((MockHttpServletResponse) response).setStatus(401);

        for (int attempt = 0; attempt < 5; attempt++) {
            MockHttpServletResponse response = invoke(filter, "/api/users/login", "10.0.0.1", failedLogin);
            assertEquals(401, response.getStatus());
        }

        MockHttpServletResponse blocked = invoke(filter, "/api/users/login", "10.0.0.1", failedLogin);
        assertEquals(429, blocked.getStatus());
        assertEquals("900", blocked.getHeader("Retry-After"));
    }

    @Test
    void successfulLoginClearsTheClientWindow() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
        FilterChain successfulLogin = (request, response) -> ((MockHttpServletResponse) response).setStatus(200);
        FilterChain failedLogin = (request, response) -> ((MockHttpServletResponse) response).setStatus(401);

        for (int attempt = 0; attempt < 4; attempt++) {
            invoke(filter, "/api/users/login", "10.0.0.2", failedLogin);
        }
        invoke(filter, "/api/users/login", "10.0.0.2", successfulLogin);

        MockHttpServletResponse response = invoke(filter, "/api/users/login", "10.0.0.2", failedLogin);
        assertEquals(401, response.getStatus());
    }

    @Test
    void concurrentAttemptsCannotExceedTheConfiguredLimit() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
        FilterChain failedLogin = (request, response) -> ((MockHttpServletResponse) response).setStatus(401);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<java.util.concurrent.Future<MockHttpServletResponse>> results = new ArrayList<>();
        try {
            for (int attempt = 0; attempt < 10; attempt++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return invoke(filter, "/api/users/login", "10.0.0.3", failedLogin);
                }));
            }
            start.countDown();
            long allowed = 0;
            long blocked = 0;
            for (var result : results) {
                if (result.get().getStatus() == 401) {
                    allowed++;
                } else if (result.get().getStatus() == 429) {
                    blocked++;
                }
            }
            assertEquals(5, allowed);
            assertEquals(5, blocked);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void forwardedHeadersCannotOverrideTheSocketAddress() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
        FilterChain failedLogin = (request, response) -> ((MockHttpServletResponse) response).setStatus(401);

        for (int attempt = 0; attempt < 5; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/login");
            request.setRemoteAddr("192.0.2.20");
            request.addHeader("X-Forwarded-For", "198.51.100." + (attempt + 1));
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, failedLogin);
            assertEquals(401, response.getStatus());
        }

        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/users/login");
        blockedRequest.setRemoteAddr("192.0.2.20");
        blockedRequest.addHeader("X-Forwarded-For", "203.0.113.10");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(blockedRequest, blockedResponse, failedLogin);

        assertEquals(429, blockedResponse.getStatus());
        assertEquals("900", blockedResponse.getHeader("Retry-After"));
    }

    private MockHttpServletResponse invoke(AuthRateLimitFilter filter, String path, String address,
            FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(address);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }
}
