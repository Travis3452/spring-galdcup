package com.example.galdcup.board.vote.scheduler;

import com.example.galdcup.board.vote.redis.VoteRedisManager;
import com.example.galdcup.board.voteSession.VoteSessionService;
import com.example.galdcup.board.voteSession.domain.VoteSession;
import com.example.galdcup.board.voteSession.domain.VoteSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * 투표 데이터 정합성 유지 및 세션 생명주기 관리 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoteSyncScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final VoteSessionRepository voteSessionRepository;
    private final VoteSessionService voteSessionService;
    private final VoteRedisManager voteRedisManager;

    private static final String DIRTY_SESSIONS_KEY = "galdcup:vote:dirty-sessions";

    /**
     * 주기적 동기화
     */
    @Scheduled(fixedRate = 60000)
    public void syncVotesToDb() {
        Set<Object> dirtySessionIds = redisTemplate.opsForSet().members(DIRTY_SESSIONS_KEY);
        if (dirtySessionIds == null || dirtySessionIds.isEmpty()) return;

        List<Long> ids = dirtySessionIds.stream()
                .map(id -> Long.valueOf(id.toString()))
                .toList();

        List<VoteSession> sessions = voteSessionRepository.findAllById(ids);

        for (VoteSession session : sessions) {
            try {
                voteSessionService.syncRedisVotesToDb(session);
                redisTemplate.opsForSet().remove(DIRTY_SESSIONS_KEY, session.getId().toString());
            } catch (Exception e) {
                log.error("투표 동기화 중 오류 발생 - 세션 ID: {}", session.getId(), e);
            }
        }
    }

    /**
     * 종료 세션 처리
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void finishVoteSessions() {
        List<VoteSession> endedSessions =
                voteSessionRepository.findByEndTimeBeforeAndIsFinishedFalse(OffsetDateTime.now());

        for (VoteSession session : endedSessions) {
            try {
                session.complete();
                voteSessionRepository.saveAndFlush(session);
                voteSessionService.syncRedisVotesToDb(session);
                voteRedisManager.deleteVoteCounts(session.getId());

                redisTemplate.opsForSet().remove(DIRTY_SESSIONS_KEY, session.getId().toString());

                log.info("투표 세션 자동 종료 완료: {}", session.getId());
            } catch (Exception e) {
                log.error("세션 {} 종료 처리 중 예외 발생", session.getId(), e);
            }
        }
    }
}