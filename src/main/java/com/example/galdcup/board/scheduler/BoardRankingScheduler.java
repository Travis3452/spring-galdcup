package com.example.galdcup.board.scheduler;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.BoardRepository;
import com.example.galdcup.board.dto.BoardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BoardRankingScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final BoardRepository boardRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRate = 300000)
    public void updateDailyBoardRanking() {
        String viewsKey = "boards:views";
        String rankingCacheKey = "boards:ranking:cache";

        try {
            Set<ZSetOperations.TypedTuple<String>> topBoards =
                    redisTemplate.opsForZSet().reverseRangeWithScores(viewsKey, 0, 99);

            if (topBoards != null && !topBoards.isEmpty()) {
                List<Long> boardIds = topBoards.stream()
                        .map(ZSetOperations.TypedTuple::getValue)
                        .filter(Objects::nonNull)
                        .map(Long::valueOf)
                        .toList();

                List<Board> boards = boardRepository.findAllById(boardIds);

                Map<Long, Board> boardMap = boards.stream()
                        .collect(Collectors.toMap(Board::getId, b -> b));

                List<BoardDto> boardDtos = boardIds.stream()
                        .map(boardMap::get)
                        .filter(Objects::nonNull)
                        .map(BoardDto::from)
                        .toList();

                String serialized = objectMapper.writeValueAsString(boardDtos);
                redisTemplate.opsForValue().set(rankingCacheKey, serialized);
            }

            redisTemplate.delete(viewsKey);

        } catch (Exception ignored) {
        }
    }
}

