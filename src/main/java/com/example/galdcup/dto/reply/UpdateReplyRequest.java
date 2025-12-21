package com.example.galdcup.dto.reply;

import jakarta.validation.constraints.NotBlank;

public record UpdateReplyRequest(
        @NotBlank String content
) {}