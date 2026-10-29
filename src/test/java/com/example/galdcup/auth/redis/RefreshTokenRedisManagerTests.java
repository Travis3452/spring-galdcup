package com.example.galdcup.auth.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class RefreshTokenRedisManagerTests {

    private final RedisTemplate<String, String> redis = mock(RedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final RefreshTokenRedisManager manager = new RefreshTokenRedisManager(redis);
    private static final String KEY = "galdcup:auth:refresh-token:1";

    @Test
    void missingStoredTokenIsAnAuthenticationFailure() {
        when(redis.opsForValue()).thenReturn(values);

        assertThrows(BadCredentialsException.class,
                () -> manager.validateRefreshToken(1L, "presented-token"));
    }

    @Test
    void differentStoredTokenIsAnAuthenticationFailure() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(KEY)).thenReturn("replacement-token");

        assertThrows(BadCredentialsException.class,
                () -> manager.validateRefreshToken(1L, "presented-token"));
    }

    @Test
    void matchingStoredTokenIsAccepted() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(KEY)).thenReturn("presented-token");

        assertDoesNotThrow(() -> manager.validateRefreshToken(1L, "presented-token"));
    }
}
