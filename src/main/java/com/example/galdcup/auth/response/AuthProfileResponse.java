package com.example.galdcup.auth.response;


import com.example.galdcup.user.domain.User;
import lombok.Builder;

@Builder
public record AuthProfileResponse(
        Long userId,
        String nickname,
        String role
) {
    public static AuthProfileResponse from(User user) {
        return AuthProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .build();
    }
}
