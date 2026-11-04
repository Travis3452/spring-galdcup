package com.example.galdcup.board.ban;

import com.example.galdcup.board.board.domain.Board;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "board_bans", indexes = @Index(name = "ix_board_ban_user", columnList = "board_id,user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private Long createdBy;

    private OffsetDateTime revokedAt;
    private Long revokedBy;

    public static BoardBan create(
            Board board, Long userId, String nickname, String reason,
            OffsetDateTime until, Long actor, OffsetDateTime now
    ) {
        if (until != null && !until.isAfter(now))
            throw new IllegalArgumentException("차단 종료 시각은 현재보다 이후여야 합니다.");

        BoardBan b = new BoardBan();
        b.board = board;
        b.userId = userId;
        b.nickname = nickname;
        b.reason = reason;
        b.expiresAt = until;
        b.createdBy = actor;
        b.createdAt = now;
        return b;
    }

    public boolean isActiveAt(OffsetDateTime now) {
        return revokedAt == null && (expiresAt == null || now.isBefore(expiresAt));
    }

    public void revoke(Long actor, OffsetDateTime now) {
        if (revokedAt == null) {
            revokedAt = now;
            revokedBy = actor;
        }
    }
}
