package com.example.galdcup.vote;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteSessionRepository extends JpaRepository<VoteSession, Long> {
}
