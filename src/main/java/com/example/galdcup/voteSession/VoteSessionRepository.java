package com.example.galdcup.voteSession;

import com.example.galdcup.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface VoteSessionRepository extends JpaRepository<VoteSession, Long> {

    List<VoteSession> findByEndTimeBeforeAndIsFinishedFalse(OffsetDateTime now);

    boolean existsByBoardAndIsFinishedFalse(Board board);

    Optional<VoteSession> findByBoardAndIsFinishedFalse(Board board);
}
