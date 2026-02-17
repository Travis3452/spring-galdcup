package com.example.galdcup.vote;

import com.example.galdcup.voteSession.VoteSession;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vote_options")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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
    private long count = 0;
}