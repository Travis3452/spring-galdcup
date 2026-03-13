package com.example.galdcup.board.event;

import com.example.galdcup.board.BoardRedisManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardEventListener {

    private final BoardRedisManager boardRedisManager;

    /**
     * BoardChangedEvent를 구독하여 캐시 무효화 실행
     */
    @Async
    @EventListener
    public void handleBoardChangedEvent(BoardChangedEvent event) {
        boardRedisManager.deleteBoardDetailCache(event.getBoardId());
        boardRedisManager.deleteBoardListCache();
    }
}