package com.example.galdcup.vote;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Table(
        name = "galdcup_votes",
        indexes = {
                @Index(name = "idx_vote_session_user", columnList = "voteSession_id, voter_id", unique = true)
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

    @Column(nullable = false)
    private Long voterId;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}