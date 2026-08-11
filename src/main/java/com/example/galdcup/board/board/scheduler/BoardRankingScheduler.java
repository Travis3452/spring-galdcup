package com.example.galdcup.board.board.scheduler;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.board.domain.BoardRepository;
import com.example.galdcup.board.board.response.BoardDto;
import com.example.galdcup.board.board.response.BoardListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 실시간 조회수 데이터를 바탕으로 주기적으로 인기 게시판 랭킹을 담당하는 스케쥴러.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoardRankingScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final BoardRepository boardRepository;

    /** Redis 키 구분자 */
    private static final String VIEWS_KEY = "galdcup:boards:views";
    private static final String RANKING_KEY = "galdcup:boards:list:ranking";

    /**
     * 5분마다 실시간 조회수를 기준으로 인기 게시판 순위 캐시를 갱신하고 카운트를 초기화합니다.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void updateDailyBoardRanking() {
        try {
            // 1. Redis ZSet에서 조회수가 높은 상위 100개 조회
            Set<ZSetOperations.TypedTuple<Object>> topBoards =
                    redisTemplate.opsForZSet().reverseRangeWithScores(VIEWS_KEY, 0, 99);

            if (topBoards == null || topBoards.isEmpty()) {
                return;
            }

            List<Long> boardIds = topBoards.stream()
                    .map(tuple -> Long.valueOf(tuple.getValue().toString()))
                    .toList();

            // 2. DB에서 실제 게시판 정보를 일괄 조회
            List<Board> boards = boardRepository.findAllById(boardIds);

            // 3. OPEN 상태인 게시판만 필터링하여 Map으로 변환
            Map<Long, Board> boardMap = boards.stream()
                    .filter(b -> b.getStatus() == Board.Status.OPEN)
                    .collect(Collectors.toMap(Board::getId, b -> b));

            // 4. Redis의 랭킹 순서를 유지하며 DTO 리스트 생성
            List<BoardDto> boardDtos = boardIds.stream()
                    .map(boardMap::get)
                    .filter(Objects::nonNull)
                    .map(BoardDto::from)
                    .toList();

            // 5. 산정된 랭킹 결과를 Redis에 캐싱
            redisTemplate.opsForValue().set(RANKING_KEY, new BoardListResponse(boardDtos), Duration.ofMinutes(10));

            // 6. 집계 완료된 실시간 카운트 초기화
            redisTemplate.unlink(VIEWS_KEY);

            log.info("정기 게시판 랭킹 업데이트 완료 (대상: {}건)", boardDtos.size());

        } catch (Exception e) {
            log.error("게시판 랭킹 업데이트 중 오류 발생: {}", e.getMessage());
        }
    }
}