package com.example.galdcup.auth.response;

import com.example.galdcup.user.domain.User;
import lombok.Builder;

@Builder
public record AuthDto(
        String refreshToken,
        long refreshTokenMaxAge,
        AuthResponse profile
) {
    public static AuthDto from(User user, String accessToken, String refreshToken, long maxAge) {
        return AuthDto.builder()
                .refreshToken(refreshToken)
                .refreshTokenMaxAge(maxAge)
                .profile(AuthResponse.of(user, accessToken)) // AuthResponse 안으로 accessToken이 쏙 들어감
                .build();
    }
}