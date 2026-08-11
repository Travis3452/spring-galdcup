package com.example.galdcup.board.comment.validator;

import com.example.galdcup.board.comment.domain.Comment;
import com.example.galdcup.board.comment.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 댓글 관련 비즈니스 규칙 및 권한 유효성 검증 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class CommentValidator {
    private final CommentRepository commentRepository;

    /** 댓글 존재 여부 확인 및 엔티티 반환 */
    public Comment findByIdOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글"));
    }

    /** 삭제 처리되지 않은 정상 댓글인지 확인 */
    public void validateNotDeleted(Comment comment) {
        if (comment.isDeleted()) {
            throw new IllegalStateException("이미 삭제된 댓글");
        }
    }

    /** 작성자 본인 일치 여부 검증 */
    public void validateIsAuthor(Comment comment, Long authorId) {
        if (!comment.getAuthor().getId().equals(authorId)) {
            throw new SecurityException("작성자 권한 불일치");
        }
    }

    /** 부모 댓글 존재 여부 검증 및 반환 */
    public Comment getValidatedParentComment(Long parentCommentId, Long postId) {
        Comment parentComment = this.findByIdOrThrow(parentCommentId);

        if (!parentComment.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("게시글 정보가 일치하지 않는 부모 댓글");
        }

        return parentComment;
    }
}