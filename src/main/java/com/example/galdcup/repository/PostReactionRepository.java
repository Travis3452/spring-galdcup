package com.example.galdcup.repository;

import com.example.galdcup.entity.Post;
import com.example.galdcup.entity.PostReaction;
import com.example.galdcup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    Optional<PostReaction> findByPostAndUser(Post post, User user);
    long countByPostIdAndType(Long postId, PostReaction.ReactionType type);
}
