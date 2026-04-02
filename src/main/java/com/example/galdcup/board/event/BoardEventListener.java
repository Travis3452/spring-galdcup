package com.example.galdcup.board.event;

import com.example.galdcup.board.redis.BoardRedisManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 게시판 관련 이벤트가 발생할 경우를 담당하는 이벤트 리스너.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoardEventListener {

    private final BoardRedisManager boardRedisManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBoardChangedEvent(BoardChangedEvent event) {
        Long boardId = event.getBoardId();

        boardRedisManager.deleteBoardDetailCache(boardId);
        boardRedisManager.deleteBoardListCache();
    }
}