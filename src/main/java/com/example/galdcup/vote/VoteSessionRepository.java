package com.example.galdcup.vote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface VoteSessionRepository extends JpaRepository<VoteSession, Long> {

    List<VoteSession> findByEndTimeBeforeAndIsFinishedFalse(OffsetDateTime now);
}
