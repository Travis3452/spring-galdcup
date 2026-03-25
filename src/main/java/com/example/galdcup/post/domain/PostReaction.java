package com.example.galdcup.post.domain;

import com.example.galdcup.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 게시글 좋아요/싫어요 관리 엔티티
 */
@Entity
@Table(name = "post_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostReaction {

    /** 반응 유형 (좋아요/싫어요) */
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

    /** 저장 전 한국 표준시(KST) 기준 생성 시각 설정 */
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