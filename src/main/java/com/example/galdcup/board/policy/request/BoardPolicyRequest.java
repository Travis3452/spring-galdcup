package com.example.galdcup.board.policy.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class BoardPolicyRequest {

    public record UpdatePolicy(
            @Min(value = 1, message = "좋아요 기준은 1 이상이어야 합니다.")
            int likeThreshold
    ) {}

    public record ManageSubManager(
            @NotBlank(message = "닉네임은 필수입니다.")
            String nickname
    ) {}

    /** 관리자 위임 요청 */
    public record Delegate(
            @NotBlank(message = "위임할 유저의 닉네임은 필수입니다.")
            String nickname
    ) {}
}
