package com.example.galdcup.service;

import com.example.galdcup.entity.Comment;
import com.example.galdcup.entity.Post;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.CommentRepository;
import com.example.galdcup.repository.PostRepository;
import com.example.galdcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // 게시글의 댓글 전체 조회
    public List<Comment> findByPost(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    // 사용자의 댓글 전체 조회 (nickname 기반)
    public List<Comment> findByAuthorNickname(String nickname) {
        return commentRepository.findByAuthorNickname(nickname);
    }

    // 특정 댓글 조회
    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    // 댓글 작성 (인증 필요)
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
    public void delete(Long id, String oauthId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getOauthId().equals(oauthId)) {
            throw new SecurityException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }
}