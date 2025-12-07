package com.example.galdcup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "boards")
@Getter
@Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Board {

    public enum Status { OPEN, CLOSED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 Topic에 속하는 Board인지 (1:1)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false, unique = true)
    private GaldcupTopic topic;

    // 게시판 상태 (OPEN, CLOSED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        status = Status.OPEN;
    }

    // 게시판 닫기 로직
    public void closeBoard() {
        this.status = Status.CLOSED;
    }
}