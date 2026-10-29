package com.example.galdcup.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTests {

    private static final String SECRET = "test-only-signing-key-0123456789abcdef0123456789abcdef0123456789";
    private final SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3600000L, 7);

    @Test
    void issuedTokensHaveDistinctPurposes() {
        String accessToken = provider.createAccessToken(1L, List.of("ADMIN"));
        String refreshToken = provider.createRefreshToken(1L);

        assertEquals("access", claims(accessToken).get("token_use", String.class));
        assertEquals("refresh", claims(refreshToken).get("token_use", String.class));
        assertEquals(1L, provider.getUserIdFromRefreshToken(refreshToken));

        var authentication = provider.getAccessAuthentication(accessToken);
        assertEquals("1", authentication.getName());
        assertTrue(authentication.isAuthenticated());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void refreshTokenCannotAuthenticateApiRequests() {
        String token = provider.createRefreshToken(1L);

        assertThrows(JwtException.class, () -> provider.getAccessAuthentication(token));
    }

    @Test
    void accessTokenCannotRefreshASession() {
        String token = provider.createAccessToken(1L, List.of("USER"));

        assertThrows(BadCredentialsException.class, () -> provider.getUserIdFromRefreshToken(token));
    }

    @Test
    void legacyTokensWithoutPurposeAreRejectedForBothUses() {
        String token = signedToken(null, List.of("ADMIN"), "1", futureExpiration());

        assertThrows(JwtException.class, () -> provider.getAccessAuthentication(token));
        assertThrows(BadCredentialsException.class, () -> provider.getUserIdFromRefreshToken(token));
    }

    @Test
    void expiredTokensAreRejectedForBothUses() {
        Date expired = new Date(System.currentTimeMillis() - 60000);
        String access = signedToken("access", List.of("ADMIN"), "1", expired);
        String refresh = signedToken("refresh", null, "1", expired);

        assertThrows(JwtException.class, () -> provider.getAccessAuthentication(access));
        assertThrows(BadCredentialsException.class, () -> provider.getUserIdFromRefreshToken(refresh));
    }

    @Test
    void tokensWithoutExpirationAreRejectedForBothUses() {
        String access = signedToken("access", List.of("ADMIN"), "1", null);
        String refresh = signedToken("refresh", null, "1", null);

        assertThrows(JwtException.class, () -> provider.getAccessAuthentication(access));
        assertThrows(BadCredentialsException.class, () -> provider.getUserIdFromRefreshToken(refresh));
    }

    @Test
    void accessTokenWithoutRolesDoesNotReceiveDefaultUserAuthority() {
        String token = signedToken("access", null, "1", futureExpiration());

        assertThrows(JwtException.class, () -> provider.getAccessAuthentication(token));
    }

    @Test
    void malformedAndUnknownRolesAreRejected() {
        for (Object roles : List.of(List.of(), List.of(123), List.of("UNKNOWN"), "ADMIN")) {
            String token = signedToken("access", roles, "1", futureExpiration());
            assertThrows(JwtException.class, () -> provider.getAccessAuthentication(token));
        }
    }

    @Test
    void invalidUserIdsAreRejectedForBothUses() {
        for (String subject : new String[]{null, "not-a-number", "0", "-1"}) {
            String access = signedToken("access", List.of("USER"), subject, futureExpiration());
            String refresh = signedToken("refresh", null, subject, futureExpiration());
            assertThrows(JwtException.class, () -> provider.getAccessAuthentication(access));
            assertThrows(BadCredentialsException.class, () -> provider.getUserIdFromRefreshToken(refresh));
        }
    }

    @Test
    void tokensSignedWithAnotherKeyAreRejected() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "other-test-signing-key-0123456789abcdef0123456789abcdef0123456789", 3600000L, 7);
        String access = otherProvider.createAccessToken(1L, List.of("ADMIN"));
        String refresh = otherProvider.createRefreshToken(1L);

        assertThrows(JwtException.class, () -> provider.getAccessAuthentication(access));
        assertThrows(BadCredentialsException.class, () -> provider.getUserIdFromRefreshToken(refresh));
    }

    @Test
    void missingAndMalformedTokensAreRejected() {
        for (String token : new String[]{null, "", "not.a.jwt"}) {
            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> provider.getAccessAuthentication(token));
            assertTrue(failure instanceof JwtException || failure instanceof IllegalArgumentException);
            assertThrows(BadCredentialsException.class, () -> provider.getUserIdFromRefreshToken(token));
        }
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    private String signedToken(String purpose, Object roles, String subject, Date expiration) {
        return Jwts.builder()
                .subject(subject)
                .claim("token_use", purpose)
                .claim("roles", roles)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    private Date futureExpiration() {
        return new Date(System.currentTimeMillis() + 60000);
    }
}
