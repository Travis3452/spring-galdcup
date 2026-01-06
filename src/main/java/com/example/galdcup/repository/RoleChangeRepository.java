package com.example.galdcup.repository;

import com.example.galdcup.entity.RoleChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleChangeRepository extends JpaRepository<RoleChange, Long> {
    List<RoleChange> findByStatus(RoleChange.Status status);
}