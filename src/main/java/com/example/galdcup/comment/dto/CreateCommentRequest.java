package com.example.galdcup.comment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotNull Long postId,
        @Size(min = 1, max = 300) String content
) {}