package com.example.galdcup.auth.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.OffsetDateTime;

@RedisHash(value = "refreshToken")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {
    @Id
    private Long userId;

    private String token;

    private OffsetDateTime expiryDate;

    public static RefreshToken create(Long userId, String token, Long ttl) {

        return RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiryDate(OffsetDateTime.now().plusSeconds(ttl))
                .build();
    }
}