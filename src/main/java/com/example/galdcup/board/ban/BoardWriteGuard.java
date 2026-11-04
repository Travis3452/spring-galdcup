package com.example.galdcup.board.ban;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class BoardWriteGuard {

    private final BoardBanRepository bans;

    public void requireAllowed(Long boardId, Long userId) {
        bans.findActive(boardId, userId, OffsetDateTime.now(), PageRequest.of(0, 1))
                .stream().findFirst()
                .ifPresent(ban -> { throw new BoardBannedException(boardId, ban); });
    }
}
