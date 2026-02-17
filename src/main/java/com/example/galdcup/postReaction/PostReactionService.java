package com.example.galdcup.postReaction;

import com.example.galdcup.post.Post;
import com.example.galdcup.post.validator.PostValidator;
import com.example.galdcup.user.User;
import com.example.galdcup.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostReactionService {

    private final PostValidator postValidator;
    private final UserValidator userValidator;
    private final PostReactionRepository reactionRepository;

    /**
     * 게시글에 좋아요/싫어요 추가
     */
    @Transactional
    public void addReaction(Long postId, Long currentUserId, PostReaction.ReactionType type) {
        Post post = postValidator.validateAndGetPost(postId);
        User user = userValidator.validateAndGetUserById(currentUserId);

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
    }
}
