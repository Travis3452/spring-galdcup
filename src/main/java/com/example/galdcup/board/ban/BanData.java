package com.example.galdcup.board.ban;

import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

public final class BanData {

    public record Request(
            @NotNull Long userId,
            @NotBlank @Size(max = 500) String reason,
            boolean permanent,
            OffsetDateTime expiresAt
    ) {}

    public record View(
            Long id,
            Long userId,
            String nickname,
            String reason,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt,
            Long createdBy,
            OffsetDateTime revokedAt,
            Long revokedBy,
            boolean active
    ) {
        public static View from(BoardBan b) {
            return new View(
                    b.getId(),
                    b.getUserId(),
                    b.getNickname(),
                    b.getReason(),
                    b.getCreatedAt(),
                    b.getExpiresAt(),
                    b.getCreatedBy(),
                    b.getRevokedAt(),
                    b.getRevokedBy(),
                    b.isActiveAt(OffsetDateTime.now())
            );
        }
    }
}
