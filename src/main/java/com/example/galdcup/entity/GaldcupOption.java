package com.example.galdcup.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(
        name = "galdcup_options",
        indexes = {
                @Index(name = "idx_option_session_label", columnList = "vote_session_id, label", unique = true)
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GaldcupOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 투표 세션에 속하는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voteSession_id", nullable = false)
    private VoteSession voteSession;

    // 선택지 이름 (예: 손흥민, 박지성)
    @Column(nullable = false, length = 50)
    private String label;

    // 선택지 설명 (선택적)
    @Column(length = 255)
    private String description;

    // 해당 옵션에 대한 투표들
    @OneToMany(mappedBy = "galdcupOption", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vote> votes;
}
