package com.example.galdcup.auth.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 리프레시 토큰의 Redis 생명주기를 관리하는 매니저.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenRedisManager {

    private final RedisTemplate<String, String> redisTemplate;

    /** Redis 키 구분자 */
    private static final String REFRESH_TOKEN_PREFIX = "galdcup:auth:refresh-token:";

    /**
     * 저장된 토큰을 조회하고 입력값과 일치하는지 검증합니다.
     */
    public void validateRefreshToken(Long userId, String refreshTokenStr) {
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);

        if (storedToken == null || !storedToken.equals(refreshTokenStr)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }
    }

    /**
     * @param ttlSeconds Redis TTL 설정을 위한 토큰 만료 시간 (초)
     */
    public void saveRefreshToken(Long userId, String token, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                token,
                Duration.ofSeconds(ttlSeconds)
        );
    }

    /** 보안상의 이유로 토큰을 즉시 무효화(로그아웃 등)할 때 사용합니다. */
    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }
}