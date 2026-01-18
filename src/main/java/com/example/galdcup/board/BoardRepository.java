package com.example.galdcup.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @Modifying
    @Query("UPDATE Board b SET b.boardPolicy.boardManager.id = NULL WHERE b.boardPolicy.boardManager.id = :userId")
    void removeBoardManagerByUserId(@Param("userId") Long userId);

    @Query("SELECT b FROM Board b WHERE b.topic LIKE %:keyword%")
    Page<Board> searchBoards(@Param("keyword") String keyword, Pageable pageable);

}