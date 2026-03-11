package com.example.galdcup.board;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardViewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BOARD_VIEWS_KEY = "boards:views";

    @Async
    public void incrementViewCount(Long boardId) {
        try {
            redisTemplate.opsForZSet().incrementScore(BOARD_VIEWS_KEY, boardId.toString(), 1);
            log.info("Async increment view count for board: {}", boardId);
        } catch (Exception e) {
            log.error("Redis error during async view count increment: {}", e.getMessage());
        }
    }
}
