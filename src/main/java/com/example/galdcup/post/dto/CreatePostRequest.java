package com.example.galdcup.post.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull Long boardId,
        @Size(min = 1, max = 50) String title,
        @Size(min = 1, max = 2000) String content
) {}