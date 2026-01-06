package com.example.galdcup.dto.rolechange;

import com.example.galdcup.entity.User;

public record CreateRoleChangeRequest(User.Role requestedRole) {}