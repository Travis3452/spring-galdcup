package com.example.galdcup.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.OffsetDateTime;

/**
 * 사용자 세션 유지를 위한 Redis 기반 Refresh Token 엔티티.
 *
 * @implNote Redis의 TTL 기능을 활용해 토큰 만료를 관리하며,
 * 중복 로그인 방지 또는 세션 강제 종료 기능을 위해 UserId를 식별자로 사용합니다.
 */
@RedisHash(value = "refreshToken")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    private Long userId;

    private String token;

    /**
     * 토큰 만료 시각.
     */
    private OffsetDateTime expiryDate;

    /**
     * @param ttlSeconds Redis TTL 설정을 위한 만료 시간(초 단위).
     */
    public static RefreshToken create(Long userId, String token, Long ttlSeconds) {
        return RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiryDate(OffsetDateTime.now().plusSeconds(ttlSeconds))
                .build();
    }
}