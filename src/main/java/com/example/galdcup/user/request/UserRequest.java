package com.example.galdcup.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserRequest {

        /** 닉네임 수정 요청 */
        public record UpdateNickname(
                @NotBlank(message = "닉네임은 공백일 수 없습니다.")
                @Size(min = 1, max = 14, message = "닉네임은 1글자 이상 14글자 이하로 정해야합니다.")
                String nickname
        ) {}

        /** 권한 변경(매니저) 신청 요청 */
        public record RoleChange(
                @NotNull(message = "신청할 권한이 누락되었습니다.")
                String requestedRole
        ) {}
}