package com.example.galdcup.user.dto;

import com.example.galdcup.user.User;

public record UserDto(Long id, String email, String nickname, String role) {

    public static UserDto from(User user, String decryptedEmail) {
        return new UserDto(
                user.getId(),
                decryptedEmail,
                user.getNickname(),
                user.getRole().name()
        );
    }
}