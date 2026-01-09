package com.example.galdcup.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByBoardId(Long boardId, Pageable pageable);
    Page<Post> findByAuthorNickname(String nickname, Pageable pageable);

    @Modifying
    @Query("UPDATE Post post SET post.view = post.view + :views WHERE post.id = :id")
    void updateViewCount(@Param("id") Long id, @Param("views") long views);
}