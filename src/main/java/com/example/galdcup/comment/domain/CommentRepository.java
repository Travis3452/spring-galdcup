package com.example.galdcup.comment.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdAndParentCommentIsNull(Long postId, Pageable pageable);

    Page<Comment> findByAuthorNickname(String nickname, Pageable pageable);

    /** 특정 게시판 내의 최신 댓글 1개를 조회 */
    Optional<Comment> findTopByPostBoardIdOrderByCreatedAtDesc(Long boardId);

    /** 게시판 내 유저별 최신 댓글을 하나씩 추출하여 조회 */
    @Query(value = """
        SELECT * FROM comments 
        WHERE id IN (
            SELECT MAX(c.id) 
            FROM comments c
            JOIN posts p ON c.post_id = p.id
            WHERE p.board_id = :boardId
            GROUP BY c.author_id
        )
        ORDER BY id DESC
        """, nativeQuery = true)
    List<Comment> findTopUniqueUsersByBoardId(@Param("boardId") Long boardId, Pageable pageable);
}