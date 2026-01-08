package com.example.galdcup.rolechange.dto;

import com.example.galdcup.user.User;

public record CreateRoleChangeRequest(User.Role requestedRole) {}