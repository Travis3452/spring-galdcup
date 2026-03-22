package com.example.galdcup.post.domain;

import com.example.galdcup.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "post_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostReaction {

    public enum ReactionType { LIKE, DISLIKE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType type;

    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public static PostReaction create(Post post, User user, ReactionType type) {
        return PostReaction.builder()
                .post(post)
                .user(user)
                .type(type)
                .build();
    }
}