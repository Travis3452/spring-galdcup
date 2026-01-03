package com.example.galdcup.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
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
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @ElementCollection
    @CollectionTable(name = "vote_session_options", joinColumns = @JoinColumn(name = "vote_session_id"))
    @Column(name = "label", nullable = false)
    private List<String> options = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "vote_session_option_images", joinColumns = @JoinColumn(name = "vote_session_id"))
    @Column(name = "image_url", nullable = false)
    private List<String> optionImages = new ArrayList<>();

    @OneToMany(mappedBy = "voteSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vote> votes;
}