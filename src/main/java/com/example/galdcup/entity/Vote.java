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

    // 투표가 속한 세션
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voteSession_id", nullable = false)
    private VoteSession voteSession;

    // 유저가 선택한 옵션
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "galdcupOption_id", nullable = false)
    private GaldcupOption galdcupOption;

    // 투표한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 투표 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}