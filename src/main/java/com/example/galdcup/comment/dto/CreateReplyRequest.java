package com.example.galdcup.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReplyRequest(
        @NotNull Long commentId,
        @NotBlank String content
) {}
