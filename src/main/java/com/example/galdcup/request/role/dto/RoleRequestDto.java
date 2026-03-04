package com.example.galdcup.request.role.dto;

import com.example.galdcup.request.role.RoleRequest;
import com.example.galdcup.user.User;

public record RoleRequestDto(
        Long id,
        Long applicantId,
        String applicantNickname, // 이메일 삭제
        User.Role requestedRole,
        RoleRequest.Status status
) {
    public static RoleRequestDto from(RoleRequest roleRequest) {
        return new RoleRequestDto(
                roleRequest.getId(),
                roleRequest.getApplicant().getId(),
                roleRequest.getApplicant().getNickname(),
                roleRequest.getRequestedRole(),
                roleRequest.getStatus()
        );
    }
}