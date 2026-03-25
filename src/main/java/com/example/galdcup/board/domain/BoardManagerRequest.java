package com.example.galdcup.board.domain;

import com.example.galdcup.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저의 게시판 매니저 권한 신청 및 처리 상태를 관리하는 엔티티.
 */
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
        PENDING,  // 승인 대기 중
        APPROVED, // 승인 완료
        DENIED    // 거절됨
    }

    public static BoardManagerRequest create(Board board, User applicant, Status status) {
        return BoardManagerRequest.builder()
                .board(board)
                .applicant(applicant)
                .status(status)
                .build();
    }
}