package com.example.galdcup.post.validator;

import com.example.galdcup.post.domain.Post;
import com.example.galdcup.post.domain.PostRepository;
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

    /** 게시글 존재 여부 확인 및 엔티티 반환 */
    public Post findByIdOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글"));
    }

    /** 작성자 본인 일치 여부 검증 */
    public void validateIsAuthor(Post post, Long authorId) {
        if (!post.getAuthor().getId().equals(authorId)) {
            throw new AccessDeniedException("작성자 권한 불일치");
        }
    }
}