package com.example.galdcup.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "galdcup_topics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GaldcupTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주제 제목
    @Column(nullable = false, length = 200)
    private String topic;

    // 시작/종료 시간
    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    // 토론 게시판 (Board와 1:1 관계)
    @OneToOne(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Board board;

    // 투표 세션 (투표 관리 전담 엔티티)
    @OneToOne(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private VoteSession voteSession;

    // 결과 (투표 종료 후 집계)
    @OneToOne(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private GaldcupResult result;
}