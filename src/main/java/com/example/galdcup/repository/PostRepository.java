package com.example.galdcup.repository;

import com.example.galdcup.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByBoardId(Long boardId, Pageable pageable);
    Page<Post> findByAuthorNickname(String nickname, Pageable pageable);

}