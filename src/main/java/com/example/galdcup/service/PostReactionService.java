package com.example.galdcup.service;

import com.example.galdcup.entity.Post;
import com.example.galdcup.entity.PostReaction;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.PostReactionRepository;
import com.example.galdcup.repository.PostRepository;
import com.example.galdcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostReactionService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostReactionRepository reactionRepository;

    public void addReaction(Long postId, Long userId, PostReaction.ReactionType type) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (reactionRepository.findByPostAndUser(post, user).isPresent()) {
            throw new IllegalStateException("이미 좋아요/싫어요를 남긴 게시물입니다.");
        }

        reactionRepository.save(PostReaction.builder()
                .post(post)
                .user(user)
                .type(type)
                .build());

        if (type == PostReaction.ReactionType.LIKE) {
            post.setLike(post.getLike() + 1);
        } else {
            post.setDislike(post.getDislike() + 1);
        }
    }
}
