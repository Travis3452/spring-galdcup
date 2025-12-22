package com.example.galdcup.service;

import com.example.galdcup.entity.Comment;
import com.example.galdcup.entity.Post;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.CommentRepository;
import com.example.galdcup.repository.PostRepository;
import com.example.galdcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // 게시글의 댓글 전체 조회
    @Transactional(readOnly = true)
    public Page<Comment> findByPost(Long postId, Pageable pageable) {
        return commentRepository.findByPostId(postId, pageable);
    }

    // 사용자의 댓글 전체 조회 (nickname 기반)
    @Transactional(readOnly = true)
    public Page<Comment> findByAuthorNickname(String nickname, Pageable pageable) {
        return commentRepository.findByAuthorNickname(nickname, pageable);
    }

    // 댓글 작성 (인증 필요)
    @Transactional
    public Comment create(Long postId, String oauthId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        User author = userRepository.findByOauthId(oauthId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .content(content)
                .build();

        return commentRepository.save(comment);
    }

    // 댓글 수정
    @Transactional
    public Comment update(Long id, String oauthId, String content) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getOauthId().equals(oauthId)) {
            throw new SecurityException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        comment.setContent(content);
        return commentRepository.save(comment);
    }

    // 댓글 삭제
    @Transactional
    public void delete(Long id, String oauthId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getOauthId().equals(oauthId)) {
            throw new SecurityException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }
}