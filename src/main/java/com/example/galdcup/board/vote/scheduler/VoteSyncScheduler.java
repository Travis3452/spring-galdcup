package com.example.galdcup.board.vote.scheduler;

import com.example.galdcup.board.vote.redis.VoteRedisManager;
import com.example.galdcup.board.voteSession.VoteSessionService;
import com.example.galdcup.board.voteSession.domain.VoteSession;
import com.example.galdcup.board.voteSession.domain.VoteSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 투표 데이터 정합성 유지 및 세션 생명주기 관리 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoteSyncScheduler {

    private final VoteSessionRepository voteSessionRepository;
    private final VoteSessionService voteSessionService;
    private final VoteRedisManager voteRedisManager;

    /**
     * 주기적 동기화
     */
    @Scheduled(fixedRate = 60000)
    public void syncVotesToDb() {
        for (String sessionId : voteRedisManager.getDirtySessionIds()) {
            try {
                voteSessionService.syncRedisVotesToDb(Long.valueOf(sessionId));
            } catch (Exception e) {
                log.error("투표 동기화 중 오류 발생 - 세션 ID: {}", sessionId, e);
            }
        }
    }

    /**
     * 종료 세션 처리
     */
    @Scheduled(fixedRate = 60000)
    public void finishVoteSessions() {
        List<VoteSession> endedSessions =
                voteSessionRepository.findByEndTimeBeforeAndIsFinishedFalse(OffsetDateTime.now());

        for (VoteSession session : endedSessions) {
            try {
                voteSessionService.finishExpiredVoteSession(session.getId());

                log.info("투표 세션 자동 종료 완료: {}", session.getId());
            } catch (Exception e) {
                log.error("세션 {} 종료 처리 중 예외 발생", session.getId(), e);
            }
        }
    }
}
