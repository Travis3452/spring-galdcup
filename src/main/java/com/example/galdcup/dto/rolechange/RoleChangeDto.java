package com.example.galdcup.dto.rolechange;

import com.example.galdcup.entity.RoleChange;
import com.example.galdcup.entity.User;

public record RoleChangeDto(
        Long id,
        Long userId,
        String nickname,
        User.Role requestedRole,
        RoleChange.Status status
) {
    public static RoleChangeDto from(RoleChange roleChange) {
        return new RoleChangeDto(
                roleChange.getId(),
                roleChange.getUser().getId(),
                roleChange.getUser().getNickname(),
                roleChange.getRequestedRole(),
                roleChange.getStatus()
        );
    }
}