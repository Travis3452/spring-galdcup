package com.example.galdcup.auth;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.OffsetDateTime;

@RedisHash(value = "refreshToken")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {
    @Id
    private Long userId;

    @Indexed
    private String token;

    @TimeToLive
    private Long ttl;

    private OffsetDateTime expiryDate;

    public static RefreshToken create(Long userId, String token, Long ttl) {

        return RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiryDate(OffsetDateTime.now().plusSeconds(ttl))
                .ttl(ttl)
                .build();
    }
}