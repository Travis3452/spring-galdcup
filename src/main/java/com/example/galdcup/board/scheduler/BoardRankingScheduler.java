package com.example.galdcup.board.scheduler;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.BoardRepository;
import com.example.galdcup.board.dto.BoardDto;
import com.example.galdcup.board.dto.BoardListResponse;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class BoardRankingScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final BoardRepository boardRepository;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void updateDailyBoardRanking() {
        String viewsKey = "boards:views";
        String rankingKey = "boards:popular:ranking";

        try {
            Set<ZSetOperations.TypedTuple<Object>> topBoards =
                    redisTemplate.opsForZSet().reverseRangeWithScores(viewsKey, 0, 99);

            if (topBoards != null && !topBoards.isEmpty()) {
                List<Long> boardIds = topBoards.stream()
                        .map(tuple -> Long.valueOf(tuple.getValue().toString()))
                        .toList();

                List<Board> boards = boardRepository.findAllById(boardIds);

                Map<Long, Board> boardMap = boards.stream()
                        .filter(b -> b.getStatus() == Board.Status.OPEN)
                        .collect(Collectors.toMap(Board::getId, b -> b));

                List<BoardDto> boardDtos = boardIds.stream()
                        .map(boardMap::get)
                        .filter(Objects::nonNull)
                        .map(BoardDto::from)
                        .toList();

                BoardListResponse response = new BoardListResponse(boardDtos);
                redisTemplate.opsForValue().set(rankingKey, response, Duration.ofMinutes(10));
            }

            redisTemplate.delete(viewsKey);

        } catch (Exception ignored) { }
    }
}