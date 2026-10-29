package com.example.galdcup.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTests {

    private final JwtTokenProvider provider = new JwtTokenProvider(
            "test-only-signing-key-0123456789abcdef0123456789abcdef0123456789", 3600000L, 7);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accessTokenAuthenticatesRequest() throws Exception {
        sendRequest(provider.createAccessToken(1L, List.of("USER")));

        assertEquals("1", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void refreshTokenCannotPopulateSecurityContext() throws Exception {
        sendRequest(provider.createRefreshToken(1L));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void malformedTokenCannotPopulateSecurityContext() throws Exception {
        sendRequest("not.a.jwt");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void requestWithoutTokenRemainsAnonymous() throws Exception {
        sendRequest(null);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private void sendRequest(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/posts");
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertSame(request, chain.getRequest());
    }
}
