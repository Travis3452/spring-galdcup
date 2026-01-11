package com.example.galdcup.request.role.dto;

import com.example.galdcup.user.User;

public record CreateRoleRequest(User.Role requestedRole) {}