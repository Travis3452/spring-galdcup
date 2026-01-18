package com.example.galdcup.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "닉네임은 공백일 수 없습니다.")
        @Size(min = 1, max = 14, message = "닉네임은 1글자 이상 14글자 이하로 정해야합니다.")
        String nickname
) {}

