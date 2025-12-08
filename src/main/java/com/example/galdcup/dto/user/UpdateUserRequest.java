package com.example.galdcup.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email @Size(max = 50)
        String email,

        @Size(min = 2, max = 20)
        String nickname
) {}

