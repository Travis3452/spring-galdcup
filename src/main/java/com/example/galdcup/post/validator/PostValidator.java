package com.example.galdcup.post.validator;

import com.example.galdcup.post.Post;
import com.example.galdcup.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostValidator {
    private final PostRepository postRepository;

    public Post validateAndGetPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }

    public void validatePostAuthor(Post post, Long authorId) {
        if (!post.getAuthor().getId().equals(authorId)) {
            throw new AccessDeniedException("이 게시글의 작성자가 아닙니다.");
        }
    }
}
