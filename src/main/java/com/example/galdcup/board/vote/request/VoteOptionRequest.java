package com.example.galdcup.board.vote.request;

import jakarta.validation.constraints.NotBlank;

public record VoteOptionRequest(
        @NotBlank(message = "선택지 이름은 필수입니다.")
        String label,

        @NotBlank(message = "선택지 이미지는 필수입니다.")
        String imageUrl
) {}