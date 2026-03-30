package com.example.galdcup.voteSession.domain;

import com.example.galdcup.board.domain.Board;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface VoteSessionRepository extends JpaRepository<VoteSession, Long> {

    List<VoteSession> findByEndTimeBeforeAndIsFinishedFalse(OffsetDateTime now);

    boolean existsByBoardAndIsFinishedFalse(Board board);

    @EntityGraph(attributePaths = {"options", "board"})
    Optional<VoteSession> findWithOptionsAndBoardById(Long id);

    /** 게시판의 가장 최신 투표 세션 1개 조회 */
    @EntityGraph(attributePaths = {"options"})
    Optional<VoteSession> findTopByBoardIdOrderByEndTimeDesc(Long boardId);

    /** 진행 중인 세션 조회 */
    @EntityGraph(attributePaths = {"options"})
    Optional<VoteSession> findByBoardIdAndIsFinishedFalse(Long boardId);

    /** 과거 세션 목록 조회(VoteOptions 포함) */
    @EntityGraph(attributePaths = {"options"})
    Page<VoteSession> findByBoardAndIsFinishedTrue(Board board, Pageable pageable);

    @EntityGraph(attributePaths = {"options"})
    @Query("SELECT v FROM VoteSession v WHERE v.id = :id")
    Optional<VoteSession> findVoteSessionWithOptionsById(@Param("id") Long id);
}
