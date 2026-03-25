package com.example.galdcup.board.redis;

import com.example.galdcup.board.response.BoardDetailResponse;
import com.example.galdcup.board.response.BoardDto;
import com.example.galdcup.board.response.BoardListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 게시판 관련 데이터의 Redis 캐싱 및 실시간 통계(조회수) 관리를 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoardRedisManager {

    private final RedisTemplate<String, Object> redisTemplate;

    /** Redis 키 구분자 */
    private static final String KEY_PREFIX = "galdcup:boards:";
    private static final String DETAIL_KEY = KEY_PREFIX + "detail:";
    private static final String LIST_KEY = KEY_PREFIX + "list:";
    private static final String BOARD_VIEWS_KEY = KEY_PREFIX + "views";

    /** Redis 데이터 유효 기간 */
    private static final Duration DETAIL_TTL = Duration.ofMinutes(5);
    private static final Duration LIST_TTL = Duration.ofMinutes(10);

    public Optional<List<BoardDto>> getBoardList(String type) {
        BoardListResponse cached = (BoardListResponse) redisTemplate.opsForValue().get(LIST_KEY + type);
        return Optional.ofNullable(cached).map(BoardListResponse::getBoardDtos);
    }

    public void saveBoardList(String type, List<BoardDto> boards) {
        if (boards != null && !boards.isEmpty()) {
            redisTemplate.opsForValue().set(LIST_KEY + type, new BoardListResponse(boards), LIST_TTL);
        }
    }

    /** 최신순, 랭킹순 등 주요 목록 캐시를 일괄 삭제합니다. */
    public void deleteBoardListCache() {
        redisTemplate.delete(LIST_KEY + "latest");
        redisTemplate.delete(LIST_KEY + "ranking");
    }

    public Optional<BoardDetailResponse> getBoardDetail(Long boardId) {
        BoardDetailResponse cached = (BoardDetailResponse) redisTemplate.opsForValue().get(DETAIL_KEY + boardId);
        return Optional.ofNullable(cached);
    }

    public void saveBoardDetail(Long boardId, BoardDetailResponse detail) {
        if (detail != null) {
            redisTemplate.opsForValue().set(DETAIL_KEY + boardId, detail, DETAIL_TTL);
        }
    }

    public void deleteBoardDetailCache(Long boardId) {
        redisTemplate.delete(DETAIL_KEY + boardId);
    }

    /**
     * Redis Sorted Set을 사용하여 게시판의 조회수를 1씩 증가시킵니다.
     * @implNote 이 점수는 실시간 게시판 랭킹 산출의 기준이 됩니다.
     */
    public void incrementViewCount(Long boardId) {
        try {
            // ZSet의 score를 1 증가시킴으로써 실시간 순위 반영
            redisTemplate.opsForZSet().incrementScore(BOARD_VIEWS_KEY, boardId.toString(), 1);
        } catch (Exception e) {
            log.error("게시판 조회수 증가 처리 중 Redis 오류 발생 (ID: {}): {}", boardId, e.getMessage());
        }
    }
}