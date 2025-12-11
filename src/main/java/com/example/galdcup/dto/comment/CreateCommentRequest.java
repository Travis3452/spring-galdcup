package com.example.galdcup.dto.comment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotNull Long postId,
        @NotNull Long authorId,
        @Size(min = 1, max = 300) String content
) {}