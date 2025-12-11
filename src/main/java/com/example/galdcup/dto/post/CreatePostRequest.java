package com.example.galdcup.dto.post;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull Long boardId,
        @NotNull Long authorId,
        @Size(min = 1, max = 50) String title,
        @Size(min = 1, max = 2000) String content
) {}