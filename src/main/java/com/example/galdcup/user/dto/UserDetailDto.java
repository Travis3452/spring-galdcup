package com.example.galdcup.user.dto;

import com.example.galdcup.user.User;

public record UserDetailDto(Long id, String email, String nickname, String role) {

    public static UserDetailDto from(User user, String decryptedEmail) {
        return new UserDetailDto(
                user.getId(),
                decryptedEmail,
                user.getNickname(),
                user.getRole().name()
        );
    }
}