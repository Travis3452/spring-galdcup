package com.example.galdcup.board.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardPolicyRepository extends JpaRepository<BoardPolicy, Long> {
    Optional<BoardPolicy> findByBoardId(Long boardId);
}