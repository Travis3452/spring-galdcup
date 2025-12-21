package com.example.galdcup.dto.reply;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReplyRequest(
        @NotNull Long commentId,
        @NotBlank String content
) {}
