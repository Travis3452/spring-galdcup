package com.example.galdcup.board.domain;

import com.example.galdcup.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "board_manager_requests")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardManagerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User applicant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public enum Status {
        PENDING,
        APPROVED,
        DENIED
    }

    public static BoardManagerRequest create(Board board, User applicant, Status status) {
        return BoardManagerRequest.builder()
                .board(board)
                .applicant(applicant)
                .status(status)
                .build();
    }
}
