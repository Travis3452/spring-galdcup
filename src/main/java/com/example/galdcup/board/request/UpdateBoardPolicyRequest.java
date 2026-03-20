package com.example.galdcup.board.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateBoardPolicyRequest(
        @NotNull(message = "인기글 기준으로 설정할 좋아요 값을 입력하세요.")
        @Min(value = 1, message = "값은 1 이상이어야 합니다.")
        Long likeThreshold
) {}
