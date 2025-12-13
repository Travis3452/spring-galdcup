package com.example.galdcup.dto.board;

import jakarta.validation.constraints.NotNull;

public record CreateBoardRequest(
        @NotNull String topic,
        @NotNull Long authorId
) {}