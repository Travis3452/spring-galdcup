package com.example.galdcup.dto.board;

import com.example.galdcup.entity.Board;
import jakarta.validation.constraints.NotNull;

public record UpdateBoardRequest(
        @NotNull Board.Status status
) {}
