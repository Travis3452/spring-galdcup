package com.example.galdcup.auth.dto;

import com.example.galdcup.user.User;
import lombok.Builder;

@Builder
public record AuthDto(
        String accessToken,
        String refreshToken,
        long refreshTokenMaxAge,
        AuthProfileResponse profile
) {
    public static AuthDto from(User user, String accessToken, String refreshToken, long maxAge) {
        return AuthDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .refreshTokenMaxAge(maxAge)
                .profile(AuthProfileResponse.from(user))
                .build();
    }
}