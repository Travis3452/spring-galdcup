package com.example.galdcup.post.dto;

import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @Size(min = 1, max = 50) String title,
        @Size(min = 1, max = 2000) String content
) {}