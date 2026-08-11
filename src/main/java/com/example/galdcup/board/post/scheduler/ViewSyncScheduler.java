package com.example.galdcup.board.post.scheduler;

import com.example.galdcup.board.post.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis에 누적된 실시간 조회수를 주기적으로 DB에 동기화하는 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ViewSyncScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final PostService postService;

    /** Redis 키 구분자 */
    private static final String VIEW_KEY_PREFIX = "galdcup:posts:view:";
    private static final String VIEW_KEY_PATTERN = "galdcup:posts:view:*";


    /**
     * 10초마다 조회수 데이터 동기화 수행
     */
    @Scheduled(fixedRate = 10000)
    public void syncViewsToDb() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(VIEW_KEY_PATTERN)
                .count(100)
                .build();

        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                processViewSync(key);
            }
        } catch (Exception e) {
            log.error("[조회수 동기화] Redis 스캔 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 개별 키에 저장된 조회수 추출 및 DB 반영
     */
    private void processViewSync(String key) {
        String value = stringRedisTemplate.opsForValue().getAndDelete(key);
        if (value == null) return;

        Long postId = extractPostId(key);
        Long viewCount = Long.parseLong(value);

        try {
            // DB 조회수 업데이트
            postService.updateViewCountInDb(postId, viewCount);
            log.info("[조회수 동기화] 게시글 ID: {} - 누적 조회수 {}회 반영 완료", postId, viewCount);
        } catch (Exception e) {
            // DB 반영 실패 시 Redis에 기존 값 복구
            stringRedisTemplate.opsForValue().increment(key, viewCount);
            log.error("[조회수 동기화] DB 반영 실패로 데이터 복구 - 게시글 ID: {}, 수량: {}", postId, viewCount);
        }
    }

    /** Redis 키에서 게시글 식별자(ID) 추출 */
    private Long extractPostId(String key) {
        return Long.valueOf(key.substring(VIEW_KEY_PREFIX.length()));
    }
}