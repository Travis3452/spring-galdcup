package com.example.galdcup.post.event;

import com.example.galdcup.post.redis.PostRedisManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 게시글 이벤트 후속 처리 담당 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
class PostEventListener {

    private final PostRedisManager postRedisManager;

    /**
     * 게시글 정보 변경 시 해당 게시글 상세 및 소속 게시판 목록 캐시 무효화 수행
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostChanged(PostChangedEvent event) {
        Long postId = event.getPostId();
        Long boardId = event.getBoardId();

        log.info("게시글 변경 이벤트 수신 - 캐시 무효화 시작 (게시글 ID: {}, 게시판 ID: {})", postId, boardId);

        // 게시글 상세 캐시 삭제
        postRedisManager.deletePostDetailCache(postId);

        // 해당 게시판의 게시글 목록 캐시 삭제
        postRedisManager.deletePostListCache(boardId);

        log.info("게시글 관련 캐시 무효화 완료");
    }
}