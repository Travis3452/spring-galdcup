package com.example.galdcup.rolechange.dto;

import com.example.galdcup.rolechange.RoleChange;
import com.example.galdcup.user.User;

public record RoleChangeDto(
        Long id,
        Long userId,
        String email,
        String nickname,
        User.Role requestedRole,
        RoleChange.Status status
) {
    public static RoleChangeDto from(RoleChange roleChange, String emailDecrypted) {
        return new RoleChangeDto(
                roleChange.getId(),
                roleChange.getUser().getId(),
                emailDecrypted,
                roleChange.getUser().getNickname(),
                roleChange.getRequestedRole(),
                roleChange.getStatus()
        );
    }
}