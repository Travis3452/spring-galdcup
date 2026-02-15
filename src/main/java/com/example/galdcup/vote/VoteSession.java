package com.example.galdcup.vote;

import com.example.galdcup.board.Board;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vote_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoteSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false, unique = true)
    private Board board;

    @Column(nullable = false)
    private OffsetDateTime startTime;

    @Column(nullable = false)
    private OffsetDateTime endTime;

    @Column(nullable = false)
    private boolean isFinished = false;

    @OneToMany(mappedBy = "voteSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoteOption> options = new ArrayList<>();
}