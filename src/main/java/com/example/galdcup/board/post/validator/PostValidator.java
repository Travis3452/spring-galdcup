package com.example.galdcup.board.post.validator;

import com.example.galdcup.board.post.domain.Post;
import com.example.galdcup.board.post.domain.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 게시글 관련 비즈니스 규칙 및 권한 유효성 검증 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class PostValidator {
    private final PostRepository postRepository;

    /** 기본 조회 */
    public Post findByIdOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
    }

    /** 게시글 + 카테고리 조회 (조회 API용) */
    public Post findPostWithCategoryOrThrow(Long postId) {
        return postRepository.findPostWithCategoryById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
    }

    /** 수정/삭제/권한 체크 전용 조회 (Post + Category + Board + Policy) */
    public Post findPostWithDetailsOrThrow(Long postId) {
        return postRepository.findPostWithDetailsById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
    }

    /** 작성자 권한 검증 */
    public void validateIsAuthor(Post post, Long authorId) {
        if (!post.getAuthor().getId().equals(authorId)) {
            throw new AccessDeniedException("본인이 작성한 글만 수정/삭제할 수 있습니다.");
        }
    }
}