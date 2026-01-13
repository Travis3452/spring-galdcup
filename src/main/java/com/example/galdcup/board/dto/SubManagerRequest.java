package com.example.galdcup.board.dto;

import jakarta.validation.constraints.NotBlank;

public record SubManagerRequest(
        @NotBlank String nickname
) {}
