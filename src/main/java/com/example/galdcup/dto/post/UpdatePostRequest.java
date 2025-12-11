package com.example.galdcup.dto.post;

import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @Size(min = 1, max = 50) String title,
        @Size(min = 1, max = 2000) String content
) {}