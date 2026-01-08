package com.example.galdcup.board.dto;

import jakarta.validation.constraints.NotNull;

public record CreateBoardRequest(
        @NotNull String topic,
        String description
) {}