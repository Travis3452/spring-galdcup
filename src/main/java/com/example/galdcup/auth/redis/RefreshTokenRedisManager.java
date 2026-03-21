package com.example.galdcup.auth.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenRedisManager {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "galdcup:auth:refresh-token:";

    public String getRefreshToken(Long userId, String refreshTokenStr) {
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);

        if (storedToken == null || !storedToken.equals(refreshTokenStr)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }
        return storedToken;
    }

    public void saveRefreshToken(Long userId, String token, long maxAge) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                token,
                Duration.ofSeconds(maxAge)
        );
    }

    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }
}
