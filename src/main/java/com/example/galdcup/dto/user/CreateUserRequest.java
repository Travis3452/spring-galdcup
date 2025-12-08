package com.example.galdcup.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 100)
        String oauthId,

        @Email @Size(max = 50)
        String email,

        @NotBlank @Size(min = 2, max = 20)
        String nickname
) {}
