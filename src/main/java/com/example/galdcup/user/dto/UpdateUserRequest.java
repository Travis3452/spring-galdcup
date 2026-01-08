package com.example.galdcup.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 2, max = 20)
        String nickname
) {}

