package com.example.galdcup.comment.validator;

import com.example.galdcup.comment.Comment;
import com.example.galdcup.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentValidator {
    private final CommentRepository commentRepository;

    public Comment validateAndGetComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
    }

    public void validateNotDeleted(Comment comment) {
        if (comment.isDeleted()) {
            throw new IllegalStateException("이미 삭제된 댓글입니다.");
        }
    }

    public void validateIsAuthor(Comment comment, Long authorId) {
        if (!comment.getAuthor().getId().equals(authorId)) {
            throw new SecurityException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }
    }
}
