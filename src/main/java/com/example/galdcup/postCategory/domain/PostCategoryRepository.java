package com.example.galdcup.postCategory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostCategoryRepository extends JpaRepository<PostCategory, Long> {

    List<PostCategory> findByBoardIdOrderBySortOrderAsc(Long boardId);

    @Query("SELECT MAX(pc.sortOrder) FROM PostCategory pc WHERE pc.board.id = :boardId")
    Optional<Integer> findMaxSortOrderByBoardId(@Param("boardId") Long boardId);

    boolean existsByBoardIdAndName(Long boardId, String name);

    Optional<PostCategory> findByBoardIdAndName(Long boardId, String name);

    List<PostCategory> findAllByBoardId(Long boardId);
}