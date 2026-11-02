package com.example.galdcup.board.board.scheduler;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.board.domain.BoardRepository;
import com.example.galdcup.board.board.redis.BoardRedisManager;
import com.example.galdcup.board.board.response.BoardDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 실시간 조회수 데이터를 바탕으로 주기적으로 인기 게시판 랭킹을 담당하는 스케쥴러.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoardRankingScheduler {

    private final BoardRepository boardRepository;
    private final BoardRedisManager boardRedisManager;

    /**
     * 5분마다 실시간 조회수를 기준으로 인기 게시판 순위 캐시를 갱신하고 카운트를 초기화합니다.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void updateDailyBoardRanking() {
        try {
            List<Long> boardIds = boardRedisManager.getTopViewBoardIds(100);

            if (boardIds.isEmpty())
                return;

            List<Board> boards = boardRepository.findAllById(boardIds);

            Map<Long, Board> boardMap = boards.stream()
                    .filter(b -> b.getStatus() == Board.Status.OPEN)
                    .collect(Collectors.toMap(Board::getId, b -> b));

            List<BoardDto> boardDtos = boardIds.stream()
                    .map(boardMap::get)
                    .filter(Objects::nonNull)
                    .map(BoardDto::from)
                    .toList();

            boardRedisManager.saveRanking(boardDtos);
            boardRedisManager.resetViewCounts();

            log.info("정기 게시판 랭킹 업데이트 완료 (대상: {}건)", boardDtos.size());

        } catch (Exception e) {
            log.error("게시판 랭킹 업데이트 중 오류 발생: {}", e.getMessage());
        }
    }
}