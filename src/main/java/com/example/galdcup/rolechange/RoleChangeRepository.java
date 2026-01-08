package com.example.galdcup.rolechange;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleChangeRepository extends JpaRepository<RoleChange, Long> {
    List<RoleChange> findByStatus(RoleChange.Status status);

    boolean existsByUserIdAndStatus(Long userId, RoleChange.Status status);
}