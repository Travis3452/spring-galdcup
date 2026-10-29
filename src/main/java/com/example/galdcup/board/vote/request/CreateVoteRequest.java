package com.example.galdcup.board.vote.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateVoteRequest(
        Long voteSessionId,
        @NotNull(message = "투표 선택지를 지정해야 합니다.")
        @PositiveOrZero(message = "유효하지 않은 투표 선택지입니다.")
        Integer selectedOptionIndex) {
}
