package com.example.galdcup.repository;

import com.example.galdcup.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByBoardId(Long boardId);
    List<Post> findByAuthorNickname(String nickname);
}