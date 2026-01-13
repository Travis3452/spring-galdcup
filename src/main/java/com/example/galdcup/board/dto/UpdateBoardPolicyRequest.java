package com.example.galdcup.board.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateBoardPolicyRequest(
        @NotNull @Min(1) Long likeThreshold
) {}
