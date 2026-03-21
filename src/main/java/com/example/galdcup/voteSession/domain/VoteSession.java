package com.example.galdcup.voteSession.domain;

import com.example.galdcup.board.domain.Board;
import com.example.galdcup.vote.domain.VoteOption;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "vote_sessions",
        indexes = {
                @Index(name = "idx_vote_session_board_status", columnList = "board_id, isFinished")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoteSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    @Builder.Default
    private boolean isFinished = false;

    @OneToMany(mappedBy = "voteSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VoteOption> options = new ArrayList<>();


    /** 세션 생성 */
    public static VoteSession create(Board board, OffsetDateTime start, OffsetDateTime end, List<VoteOption> options) {
        if (end.isBefore(start)) throw new IllegalArgumentException("종료 시간은 시작 시간보다 빨라야 합니다.");

        VoteSession session = VoteSession.builder()
                .board(board)
                .startTime(start)
                .endTime(end)
                .options(new ArrayList<>())
                .build();

        options.forEach(session::addOption);
        return session;
    }

    private void addOption(VoteOption option) {
        this.options.add(option);
        option.assignSession(this);
    }

    /** 세션 강제 종료 (관리자용) */
    public void terminate() {
        this.isFinished = true;
        this.endTime = OffsetDateTime.now();
    }

    /** 시간이 만료되어 자동 종료 처리 */
    public void complete() {
        this.isFinished = true;
    }

    /** 현재 투표 가능한 상태인지 확인 */
    public boolean isActive() {
        OffsetDateTime now = OffsetDateTime.now();
        return !isFinished && now.isAfter(startTime) && now.isBefore(endTime);
    }
}