package com.example.galdcup.post.scheduler;

import com.example.galdcup.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ViewSyncScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final PostRepository postRepository;

    /**
     * 10초마다 Redis 조회수를 DB에 반영
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void syncViewsToDb() {
        Set<String> keys = redisTemplate.keys("post:view:*");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            try {
                Long postId = Long.valueOf(key.replace("post:view:", ""));
                String value = redisTemplate.opsForValue().get(key);

                if (value != null) {
                    long views = Long.parseLong(value);

                    postRepository.incrementViewCount(postId, views);

                    redisTemplate.delete(key);
                }
            } catch (Exception e) {
                System.err.println("조회수 동기화 실패: " + key + " - " + e.getMessage());
            }
        }
    }
}