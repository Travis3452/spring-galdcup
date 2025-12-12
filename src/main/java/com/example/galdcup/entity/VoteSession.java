package com.example.galdcup.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vote_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoteSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 주제에 속하는 투표 세션인지
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "galdcupTopic_id", nullable = false, unique = true)
    private GaldcupTopic galdcupTopic;

    // 세션 시작/종료 시간
    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    // 옵션들
    @OneToMany(mappedBy = "voteSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GaldcupOption> options;

    // 투표 기록들
    @OneToMany(mappedBy = "voteSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vote> votes;

    // 결과 (투표 종료 후 집계)
    @OneToOne(mappedBy = "voteSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private GaldcupResult galdcupResult;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); }
    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }
}