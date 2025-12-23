package com.example.galdcup.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 2, max = 20)
        String nickname
) {}

