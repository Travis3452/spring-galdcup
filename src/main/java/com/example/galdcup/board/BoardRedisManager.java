package com.example.galdcup.board;

import com.example.galdcup.board.dto.BoardDetailResponse;
import com.example.galdcup.board.dto.BoardDto;
import com.example.galdcup.board.dto.BoardListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BoardRedisManager {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "galdcup:boards:";
    private static final String DETAIL_KEY = KEY_PREFIX + "detail:";
    private static final String LIST_KEY = KEY_PREFIX + "list:";

    private static final Duration DETAIL_TTL = Duration.ofHours(1);
    private static final Duration LIST_TTL = Duration.ofMinutes(5);

    public Optional<List<BoardDto>> getBoardList(String type) {
        BoardListResponse cached = (BoardListResponse) redisTemplate.opsForValue().get(LIST_KEY + type);
        return Optional.ofNullable(cached).map(BoardListResponse::getBoardDtos);
    }

    @Async
    public void saveBoardList(String type, List<BoardDto> boards) {
        if (boards != null && !boards.isEmpty()) {
            redisTemplate.opsForValue().set(LIST_KEY + type, new BoardListResponse(boards), LIST_TTL);
        }
    }

    @Async
    public void deleteBoardListCache() {
        redisTemplate.delete(LIST_KEY + "latest");
        redisTemplate.delete(LIST_KEY + "ranking");
    }

    public Optional<BoardDetailResponse> getBoardDetail(Long boardId) {
        BoardDetailResponse cached = (BoardDetailResponse) redisTemplate.opsForValue().get(DETAIL_KEY + boardId);
        return Optional.ofNullable(cached);
    }

    @Async
    public void saveBoardDetail(Long boardId, BoardDetailResponse detail) {
        if (detail != null) {
            redisTemplate.opsForValue().set(DETAIL_KEY + boardId, detail, DETAIL_TTL);
        }
    }

    @Async
    public void deleteBoardDetailCache(Long boardId) {
        redisTemplate.delete(DETAIL_KEY + boardId);
    }
}