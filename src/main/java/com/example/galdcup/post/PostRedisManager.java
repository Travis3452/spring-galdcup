package com.example.galdcup.post;

import com.example.galdcup.post.dto.PostDto;
import com.example.galdcup.post.dto.PostListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PostRedisManager {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LIST_KEY_PREFIX = "galdcup:posts:list:";
    private static final String VIEW_KEY_PREFIX = "galdcup:posts:view:";
    private static final Duration LIST_TTL = Duration.ofMinutes(5);

    /**
     * 게시글 목록 캐시 조회
     */
    public Optional<Page<PostDto>> getPostPage(Long boardId, Long categoryId, Long threshold, Pageable pageable) {
        String key = generateListKey(boardId, categoryId, threshold);
        PostListResponse cached = (PostListResponse) redisTemplate.opsForValue().get(key);

        return Optional.ofNullable(cached)
                .map(res -> new PageImpl<>(res.getPostDtos(), pageable, res.getTotalElements()));
    }

    /**
     * 게시글 목록 캐시 저장
     */
    public void savePostList(Long boardId, Long categoryId, Long threshold, List<PostDto> posts, long totalElements) {
        String key = generateListKey(boardId, categoryId, threshold);
        if (posts != null && !posts.isEmpty()) {
            redisTemplate.opsForValue().set(key, new PostListResponse(posts, totalElements), LIST_TTL);
        }
    }

    /**
     * 해당 게시판의 모든 목록 캐시 삭제
     */
    public void deletePostListCache(Long boardId) {
        redisTemplate.delete(redisTemplate.keys(LIST_KEY_PREFIX + boardId + ":*"));
    }

    /**
     * 게시글 조회수 증가
     */
    public void incrementViewCount(Long postId) {
        String key = VIEW_KEY_PREFIX + postId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
    }

    /**
     * 게시글 조회수 캐시 삭제
     */
    public void deleteViewCache(Long postId) {
        redisTemplate.delete(VIEW_KEY_PREFIX + postId);
    }

    private String generateListKey(Long boardId, Long categoryId, Long threshold) {
        String cat = (categoryId == null) ? "all" : categoryId.toString();
        String type = (threshold == null) ? "normal" : "popular";
        return LIST_KEY_PREFIX + boardId + ":" + cat + ":" + type;
    }
}