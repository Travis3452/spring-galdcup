package com.example.galdcup.auth.response;

import com.example.galdcup.user.domain.User;
import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        Long userId,
        String nickname,
        String role
) {
    public static AuthResponse of(User user, String accessToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .build();
    }
}