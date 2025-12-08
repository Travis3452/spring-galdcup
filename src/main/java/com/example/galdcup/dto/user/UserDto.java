package com.example.galdcup.dto.user;

public record UserDto(Long id, String email, String nickname, String role) {
    public static UserDto from(com.example.galdcup.entity.User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name()
        );
    }
}