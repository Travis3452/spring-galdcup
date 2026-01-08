package com.example.galdcup.comment;

import com.example.galdcup.comment.dto.ReplyDto;
import com.example.galdcup.user.User;
import com.example.galdcup.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReplyService {

    private final ReplyRepository replyRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    /**
     * 대댓글 작성
     */
    @Transactional
    public ReplyDto create(Long commentId, Long authorId, String content) {
        Comment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Reply reply = Reply.builder()
                .parentComment(parentComment)
                .author(author)
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
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("대댓글을 찾을 수 없습니다."));

        if (reply.isDeleted()) {
            throw new IllegalStateException("삭제된 대댓글은 수정할 수 없습니다.");
        }
        if (reply.getAuthor() == null || !reply.getAuthor().getId().equals(authorId)) {
            throw new SecurityException("본인이 작성한 대댓글만 수정할 수 있습니다.");
        }

        reply.setContent(content);
        return ReplyDto.from(replyRepository.save(reply));
    }

    /**
     * 대댓글 삭제 (소프트 삭제 처리)
     */
    @Transactional
    public ReplyDto delete(Long replyId, Long authorId) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("대댓글을 찾을 수 없습니다."));

        if (reply.isDeleted()) {
            throw new IllegalStateException("이미 삭제된 대댓글입니다.");
        }
        if (reply.getAuthor() == null || !reply.getAuthor().getId().equals(authorId)) {
            throw new SecurityException("본인이 작성한 대댓글만 삭제할 수 있습니다.");
        }

        reply.delete();
        return ReplyDto.from(replyRepository.save(reply));
    }
}
