package com.example.galdcup.board.event;

import com.example.galdcup.board.redis.BoardRedisManager;
import com.example.galdcup.post.redis.PostRedisManager;
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
    private final PostRedisManager postRedisManager;

    /**
     * 게시판 정보 변경 시 관련 Redis 캐시를 삭제합니다.
     * * @param event 변경된 게시판 정보를 담은 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBoardChangedEvent(BoardChangedEvent event) {
        Long boardId = event.getBoardId();
        log.info("게시판 변경 이벤트 수신 - 캐시 무효화를 시작합니다. 게시판 ID: {}", boardId);

        // 게시판 상세 및 전체 목록 캐시 삭제
        boardRedisManager.deleteBoardDetailCache(boardId);
        boardRedisManager.deleteBoardListCache();

        // 해당 게시판의 게시글 목록 캐시도 함께 정리
        postRedisManager.deletePostListCache(boardId);

        log.info("게시판 관련 캐시 무효화가 완료되었습니다. 게시판 ID: {}", boardId);
    }
}