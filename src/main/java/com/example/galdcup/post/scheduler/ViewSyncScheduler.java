package com.example.galdcup.post.scheduler;

import com.example.galdcup.post.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewSyncScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostRepository postRepository;

    private static final String VIEW_KEY_PATTERN = "galdcup:posts:view:*";
    private static final String VIEW_KEY_PREFIX = "galdcup:posts:view:";

    /**
     * 주기적으로 Redis의 조회수를 DB에 반영 (Write-Behind)
     */
    @Scheduled(fixedRate = 10000) // 10초 주기
    @Transactional
    public void syncViewsToDb() {
        ScanOptions options = ScanOptions.scanOptions().match(VIEW_KEY_PATTERN).count(100).build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                handleViewSync(key);
            }
        } catch (Exception e) {
            log.error("조회수 동기화 스케줄러 실행 중 오류 발생", e);
        }
    }

    private void handleViewSync(String key) {
        try {
            Long postId = Long.valueOf(key.replace(VIEW_KEY_PREFIX, ""));

            Object value = redisTemplate.opsForValue().getAndDelete(key);

            if (value != null) {
                long views = Long.parseLong(value.toString());
                postRepository.incrementViewCount(postId, views);
                log.debug("조회수 동기화 완료: 게시글 {}, {}회", postId, views);
            }
        } catch (Exception e) {
            log.error("개별 게시글 조회수 동기화 실패: 키 = {}", key, e);
        }
    }
}