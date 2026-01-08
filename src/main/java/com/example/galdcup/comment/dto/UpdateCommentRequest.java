package com.example.galdcup.comment.dto;

import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @Size(min = 1, max = 300) String content
) {}