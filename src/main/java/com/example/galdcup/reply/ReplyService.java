package com.example.galdcup.reply;

import com.example.galdcup.comment.Comment;
import com.example.galdcup.reply.dto.ReplyDto;
import com.example.galdcup.comment.embedded.Author;
import com.example.galdcup.comment.validator.CommentValidator;
import com.example.galdcup.reply.dto.ReplyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReplyService {

    private final ReplyRepository replyRepository;
    private final ReplyValidator replyValidator;
    private final CommentValidator commentValidator;

    /**
     * 대댓글 작성
     */
    @Transactional
    public ReplyDto create(Long commentId, Long authorId, String authorNickname, String content) {
        Comment parentComment = commentValidator.validateAndGetComment(commentId);

        Reply reply = Reply.builder()
                .parentComment(parentComment)
                .author(new Author(authorId, authorNickname))
                .content(content)
                .build();

        return ReplyDto.from(replyRepository.save(reply));
    }

    /**
     * 특정 댓글에 달린 대댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReplyDto> findByComment(Long commentId) {
        return replyRepository.findByParentCommentId(commentId)
                .stream()
                .map(ReplyDto::from)
                .toList();
    }

    /**
     * 대댓글 수정
     */
    @Transactional
    public ReplyDto update(Long replyId, Long authorId, String content) {
        Reply reply = replyValidator.validateAndGetReply(replyId);
        replyValidator.validateNotDeleted(reply);
        replyValidator.validateIsAuthor(reply, authorId);

        reply.setContent(content);
        return ReplyDto.from(reply);
    }

    /**
     * 대댓글 삭제 (소프트 삭제 처리)
     */
    @Transactional
    public ReplyDto delete(Long replyId, Long authorId) {
        Reply reply = replyValidator.validateAndGetReply(replyId);
        replyValidator.validateNotDeleted(reply);
        replyValidator.validateIsAuthor(reply, authorId);

        reply.delete();
        return ReplyDto.from(reply);
    }
}
