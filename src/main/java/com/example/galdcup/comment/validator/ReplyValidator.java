package com.example.galdcup.comment.validator;

import com.example.galdcup.comment.Reply;
import com.example.galdcup.comment.ReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplyValidator {
    private final ReplyRepository replyRepository;

    public Reply validateAndGetReply(Long replyId) {
        return replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("대댓글을 찾을 수 없습니다."));
    }

    public void validateNotDeleted(Reply reply) {
        if (reply.isDeleted()) {
            throw new IllegalStateException("이미 삭제된 대댓글입니다.");
        }
    }

    public void validateIsAuthor(Reply reply, Long authorId) {
        if (!reply.getAuthor().getId().equals(authorId)) {
            throw new SecurityException("본인이 작성한 대댓글만 수정할 수 있습니다.");
        }
    }
}
