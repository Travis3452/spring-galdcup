package com.example.galdcup.post;

import com.example.galdcup.common.redis.CachedPageResponse;
import com.example.galdcup.post.dto.PostDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostRedisManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String LIST_KEY_PREFIX = "galdcup:posts:list:";
    private static final String VIEW_KEY_PREFIX = "galdcup:posts:view:";

    private static final Duration LIST_TTL = Duration.ofMinutes(1);
    private static final int MAX_CACHE_PAGE = 5;

    /**
     * 게시글 목록 캐시 조회 (상위 5페이지만)
     */
    public Optional<CachedPageResponse<PostDto>> getPostPage(Long boardId, Long categoryId, Long threshold, Pageable pageable) {
        if (pageable.getPageNumber() >= MAX_CACHE_PAGE) {
            return Optional.empty();
        }

        String key = generateListKey(boardId, categoryId, threshold, pageable);
        try {
            CachedPageResponse<PostDto> cached = (CachedPageResponse<PostDto>) redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(cached);
        } catch (Exception e) {
            log.error("Redis 조회 중 에러 발생: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 게시글 목록 캐시 저장
     */
    public void savePostList(Long boardId, Long categoryId, Long threshold, Pageable pageable, CachedPageResponse<PostDto> response) {
        if (pageable.getPageNumber() >= MAX_CACHE_PAGE) {
            return;
        }

        String key = generateListKey(boardId, categoryId, threshold, pageable);
        redisTemplate.opsForValue().set(key, response, LIST_TTL);
    }

    /**
     * 해당 게시판의 모든 목록 캐시 삭제
     */
    public void deletePostListCache(Long boardId) {
        String pattern = LIST_KEY_PREFIX + boardId + ":*";
        Set<String> keys = redisTemplate.keys(pattern); // 주의: 키가 너무 많으면 keys가 차단될 수 있음
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.unlink(keys);
            log.info("🗑️ [Async Cache Delete] Board: {}, Count: {}", boardId, keys.size());
        }
    }

    /**
     * 게시글 조회수 증가
     */
    public void incrementViewCount(Long postId) {
        String key = VIEW_KEY_PREFIX + postId;
        stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * 게시글 조회수 캐시 삭제
     */
    public void deleteViewCache(Long postId) {
        redisTemplate.delete(VIEW_KEY_PREFIX + postId);
    }

    private String generateListKey(Long boardId, Long categoryId, Long threshold, Pageable pageable) {
        String cat = (categoryId == null) ? "all" : categoryId.toString();
        String type = (threshold == null) ? "normal" : "popular";
        return String.format("%s%d:%s:%s:%d:%d",
                LIST_KEY_PREFIX, boardId, cat, type, pageable.getPageNumber(), pageable.getPageSize());
    }
}