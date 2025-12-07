package com.example.galdcup.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "galdcup_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GaldcupResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 투표 세션의 결과인지
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_session_id", nullable = false, unique = true)
    private VoteSession voteSession;

    // 총 투표 수
    @Column(nullable = false)
    private Long totalVotes;

    // 옵션별 집계 결과 (구조화된 JSON 배열)
    // 예: [{"optionId":1,"label":"손흥민","votes":120},{"optionId":2,"label":"박지성","votes":80}]
    @Column(columnDefinition = "TEXT")
    private String optionBreakdownJson;
}