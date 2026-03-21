package com.example.galdcup.auth.response;

import com.example.galdcup.user.domain.User;
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