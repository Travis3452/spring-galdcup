package com.example.galdcup.board.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByStatus(Board.Status status);
    Page<Board> findByStatus(Board.Status status, Pageable pageable);

    @Query("SELECT b FROM Board b " +
            "JOIN FETCH b.boardPolicy " +
            "WHERE b.id = :id AND b.status = 'OPEN'")
    Optional<Board> findBoardWithPolicyById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Board b SET b.boardPolicy.boardManager.id = NULL WHERE b.boardPolicy.boardManager.id = :userId")
    void removeBoardManagerByUserId(@Param("userId") Long userId);

    @Query("SELECT b FROM Board b WHERE b.topic LIKE %:keyword%")
    Page<Board> searchBoards(@Param("keyword") String keyword, Pageable pageable);

}