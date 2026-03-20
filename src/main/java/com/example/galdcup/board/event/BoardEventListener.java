package com.example.galdcup.board.event;

import com.example.galdcup.board.redis.BoardRedisManager;
import com.example.galdcup.post.redis.PostRedisManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardEventListener {

    private final BoardRedisManager boardRedisManager;
    private final PostRedisManager postRedisManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBoardChangedEvent(BoardChangedEvent event) {
        Long boardId = event.getBoardId();
        log.info("Starting cache invalidation for boardId: {}", boardId);

        boardRedisManager.deleteBoardDetailCache(boardId);
        boardRedisManager.deleteBoardListCache();

        postRedisManager.deletePostListCache(boardId);

        log.info("Cache invalidation completed for boardId: {}", boardId);
    }
}