package com.example.galdcup.vote;

import com.example.galdcup.board.Board;
import com.example.galdcup.vote.embedded.VoteOption;
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

    @ElementCollection
    @CollectionTable(name = "vote_session_options", joinColumns = @JoinColumn(name = "vote_session_id"))
    private List<VoteOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "voteSession", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Vote> votes = new ArrayList<>();
}