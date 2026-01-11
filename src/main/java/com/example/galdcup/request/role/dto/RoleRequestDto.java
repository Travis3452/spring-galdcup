package com.example.galdcup.request.role.dto;

import com.example.galdcup.request.role.RoleRequest;
import com.example.galdcup.user.User;

public record RoleRequestDto(
        Long id,
        Long applicantId,
        String applicantEmail,
        String applicantNickname,
        User.Role requestedRole,
        RoleRequest.Status status
) {
    public static RoleRequestDto from(RoleRequest roleRequest, String decryptedEmail) {
        return new RoleRequestDto(
                roleRequest.getId(),
                roleRequest.getApplicant().getId(),
                decryptedEmail,
                roleRequest.getApplicant().getNickname(),
                roleRequest.getRequestedRole(),
                roleRequest.getStatus()
        );
    }
}