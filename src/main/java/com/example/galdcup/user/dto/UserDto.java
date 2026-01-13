package com.example.galdcup.user.dto;

import com.example.galdcup.user.User;

public record UserDto(Long id, String nickname, String role) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getNickname(),
                user.getRole().name()
        );
    }
}