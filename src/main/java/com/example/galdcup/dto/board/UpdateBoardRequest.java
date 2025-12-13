package com.example.galdcup.dto.board;

import jakarta.validation.constraints.NotNull;
import com.example.galdcup.entity.Board;

public record UpdateBoardRequest(
        @NotNull Board.Status status
) {}
