package com.example.galdcup.board.ban;

import org.springframework.security.access.AccessDeniedException;
import java.time.OffsetDateTime;

public class BoardBannedException extends AccessDeniedException {
    private final Long boardId;
    private final String reason;
    private final OffsetDateTime startsAt;
    private final OffsetDateTime expiresAt;

    public BoardBannedException(Long boardId, BoardBan ban) {
        super("게시판에서 차단된 사용자입니다.");
        this.boardId = boardId;
        this.reason = ban.getReason();
        this.startsAt = ban.getCreatedAt();
        this.expiresAt = ban.getExpiresAt();
    }

    public Long getBoardId() { return boardId; }
    public String getReason() { return reason; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}
