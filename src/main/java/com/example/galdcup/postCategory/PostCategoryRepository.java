package com.example.galdcup.postCategory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostCategoryRepository extends JpaRepository<PostCategory, Long> {
    boolean existsByBoardIdAndName(Long boardId, String name);

    List<PostCategory> findByBoardId(Long boardId);
}
