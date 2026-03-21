package com.example.galdcup.user.response;

import com.example.galdcup.user.domain.User;

public record UserDetailDto(Long id, String email, String nickname, String role) {

    public static UserDetailDto from(User user) {
        return new UserDetailDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name()
        );
    }
}