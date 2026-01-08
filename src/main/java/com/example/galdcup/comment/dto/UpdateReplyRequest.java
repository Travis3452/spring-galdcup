package com.example.galdcup.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateReplyRequest(
        @NotBlank String content
) {}