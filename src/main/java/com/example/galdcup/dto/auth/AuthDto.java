package com.example.galdcup.dto.auth;

import lombok.Builder;

@Builder
public record AuthDto(
        String accessToken,
        String refreshToken,
        int refreshTokenMaxAge,
        String nickname
) {
    public static AuthDto of(String accessToken, String refreshToken, int maxAge, String nickname) {
        return AuthDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .refreshTokenMaxAge(maxAge)
                .nickname(nickname)
                .build();
    }
}