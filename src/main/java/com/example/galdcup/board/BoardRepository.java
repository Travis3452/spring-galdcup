package com.example.galdcup.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @Modifying
    @Query("UPDATE Board b SET b.boardManager = NULL WHERE b.boardManager.id = :userId")
    void removeBoardManagerByUserId(@Param("userId") Long userId);
}