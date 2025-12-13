package com.example.galdcup.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "galdcup_votes",
        indexes = {
                @Index(name = "idx_vote_session_user", columnList = "voteSession_id, user_id", unique = true)
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voteSession_id", nullable = false)
    private VoteSession voteSession;

    @Column(nullable = false)
    private int selectedOptionIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}