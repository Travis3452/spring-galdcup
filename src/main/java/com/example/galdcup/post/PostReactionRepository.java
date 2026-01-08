package com.example.galdcup.post;

import com.example.galdcup.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    Optional<PostReaction> findByPostAndUser(Post post, User user);
    long countByPostIdAndType(Long postId, PostReaction.ReactionType type);
}
