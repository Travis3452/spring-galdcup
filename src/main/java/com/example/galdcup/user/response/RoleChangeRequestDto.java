package com.example.galdcup.user.response;

import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.role.RoleRequest;

public record RoleChangeRequestDto (
        Long id,
        Long applicantId,
        String applicantNickname,
        User.Role requestedRole,
        RoleRequest.Status status
) {
    public static RoleChangeRequestDto from(RoleRequest roleRequest) {
        return new RoleChangeRequestDto(
                roleRequest.getId(),
                roleRequest.getApplicant().getId(),
                roleRequest.getApplicant().getNickname(),
                roleRequest.getRequestedRole(),
                roleRequest.getStatus()
        );
    }
}