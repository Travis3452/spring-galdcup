package com.example.galdcup.service;

import com.example.galdcup.entity.Comment;
import com.example.galdcup.entity.Reply;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.CommentRepository;
import com.example.galdcup.repository.ReplyRepository;
import com.example.galdcup.repository.UserRepository;
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

    // 대댓글 작성
    @Transactional
    public Reply create(Long commentId, Long authorId, String content) {
        Comment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Reply reply = Reply.builder()
                .parentComment(parentComment)
                .createdBy(author)
                .content(content)
                .build();

        return replyRepository.save(reply);
    }

    // 대댓글 조회
    @Transactional(readOnly = true)
    public List<Reply> findByComment(Long commentId) {
        return replyRepository.findByParentCommentId(commentId);
    }

    // 대댓글 수정
    @Transactional
    public Reply update(Long replyId, Long authorId, String content) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("대댓글을 찾을 수 없습니다."));

        if (!reply.getCreatedBy().getId().equals(authorId)) {
            throw new SecurityException("본인이 작성한 대댓글만 수정할 수 있습니다.");
        }

        reply.setContent(content);
        return replyRepository.save(reply);
    }

    // 대댓글 삭제
    @Transactional
    public void delete(Long replyId, Long authorId) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("대댓글을 찾을 수 없습니다."));

        if (!reply.getCreatedBy().getId().equals(authorId)) {
            throw new SecurityException("본인이 작성한 대댓글만 삭제할 수 있습니다.");
        }

        replyRepository.delete(reply);
    }
}