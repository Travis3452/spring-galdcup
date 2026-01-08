package com.example.galdcup.board.dto;

import com.example.galdcup.board.Board;
import jakarta.validation.constraints.NotNull;

public record UpdateBoardRequest(
        @NotNull Board.Status status
) {}
