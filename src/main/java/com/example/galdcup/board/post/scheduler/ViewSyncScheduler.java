package com.example.galdcup.board.post.scheduler;

import com.example.galdcup.board.post.PostService;
import com.example.galdcup.board.post.redis.PostRedisManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewSyncScheduler {

    private final PostRedisManager postRedisManager;
    private final PostService postService;

    /**
     * 10초마다 Redis에 누적된 조회수를 DB에 반영합니다.
     */
    @Scheduled(fixedRate = 10000)
    public void syncViewsToDb() {
        try {
            for (Long postId : postRedisManager.findPendingViewPostIds()) {
                syncPostViews(postId);
            }
        } catch (Exception e) {
            log.error("[조회수 동기화] 작업 중 오류 발생", e);
        }
    }

    private void syncPostViews(Long postId) {
        Long viewCount = postRedisManager.takeViewCount(postId);

        if (viewCount == null) {
            return;
        }

        try {
            postService.updateViewCountInDb(postId, viewCount);
        } catch (Exception e) {
            postRedisManager.restoreViewCount(postId, viewCount);

            log.error(
                    "[조회수 동기화] DB 반영 실패로 조회수 복구 - 게시글 ID: {}, 조회수: {}",
                    postId,
                    viewCount,
                    e
            );
            return;
        }

        log.info(
                "[조회수 동기화] DB 반영 완료 - 게시글 ID: {}, 조회수: {}",
                postId,
                viewCount
        );
    }
}