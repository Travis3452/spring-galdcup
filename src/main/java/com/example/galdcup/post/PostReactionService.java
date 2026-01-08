package com.example.galdcup.post;

import com.example.galdcup.user.User;
import com.example.galdcup.user.UserRepository;
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

    /**
     * 게시글에 좋아요/싫어요 추가
     */
    public void addReaction(Long postId, Long currentUserId, PostReaction.ReactionType type) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        User user = userRepository.findById(currentUserId)
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
            post.addLike();
        } else {
            post.addDislike();
        }

        postRepository.save(post);
    }
}
