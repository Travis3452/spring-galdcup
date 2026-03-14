package com.example.galdcup.post.scheduler;

import com.example.galdcup.post.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewSyncScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final PostService postService;

    private static final String VIEW_KEY_PREFIX = "galdcup:posts:view:";
    private static final String VIEW_KEY_PATTERN = "galdcup:posts:view:*";

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
            log.error("조회수 스캔 중 오류 발생", e);
        }
    }

    private void processViewSync(String key) {
        String value = stringRedisTemplate.opsForValue().getAndDelete(key);
        if (value == null) return;

        Long postId = extractPostId(key);
        Long viewCount = Long.parseLong(value);

        try {
            postService.updateViewCountInDb(postId, viewCount);
            log.info("조회수 동기화 완료: 게시글 {}, {}회", postId, viewCount);
        } catch (Exception e) {
            stringRedisTemplate.opsForValue().increment(key, viewCount);
            log.error("DB 반영 실패로 조회수 복구: postId={}, views={}", postId, viewCount, e);
        }
    }

    private Long extractPostId(String key) {
        return Long.valueOf(key.substring(VIEW_KEY_PREFIX.length()));
    }
}