package com.example.galdcup.comment.domain;

import com.example.galdcup.comment.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByPostIdAndParentCommentIsNull(Long postId, Pageable pageable);

    @EntityGraph(attributePaths = {"post", "post.board"})
    Page<Comment> findByAuthorNickname(String nickname, Pageable pageable);
}