package com.example.galdcup.vote.domain;

import com.example.galdcup.voteSession.domain.VoteSession;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표 세션의 개별 선택지 엔티티
 */
@Entity
@Table(name = "vote_options")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoteOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_session_id", nullable = false)
    private VoteSession voteSession;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private long count = 0;

    /**
     * 투표 옵션 생성
     */
    public static VoteOption create(String label, String imageUrl) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("옵션 라벨은 필수입니다.");
        }
        return VoteOption.builder()
                .label(label)
                .imageUrl(imageUrl)
                .count(0L)
                .build();
    }

    public void updateCount(long newCount) {
        this.count = newCount;
    }

    /**
     * 투표 세션과의 연관관계 설정
     */
    public void assignSession(VoteSession voteSession) {
        this.voteSession = voteSession;
    }
}