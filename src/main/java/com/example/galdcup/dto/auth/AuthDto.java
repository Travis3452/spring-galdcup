package com.example.galdcup.dto.auth;

import lombok.Builder;

@Builder
public record AuthDto(
        String accessToken,
        String refreshToken,
        int refreshTokenMaxAge
) {
    public static AuthDto of(String accessToken, String refreshToken, int refreshTokenMaxAge) {
        return AuthDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .refreshTokenMaxAge(refreshTokenMaxAge)
                .build();
    }
}