package com.example.galdcup.auth;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.OffsetDateTime;

@RedisHash(value = "refreshToken", timeToLive = 604800)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {
    @Id
    private Long userId;

    @Indexed
    private String token;

    private OffsetDateTime expiryDate;
}