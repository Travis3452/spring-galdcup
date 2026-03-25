package com.example.galdcup.vote.scheduler;

import com.example.galdcup.vote.redis.VoteRedisManager;
import com.example.galdcup.voteSession.VoteSessionService;
import com.example.galdcup.voteSession.domain.VoteSession;
import com.example.galdcup.voteSession.domain.VoteSessionRepository;
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

    /**
     * 주기적 동기화
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncVotesToDb() {
        Set<String> keys = redisTemplate.keys("voteSession:count:*");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            try {
                Long voteSessionId = Long.valueOf(key.split(":")[2]);
                voteSessionRepository.findById(voteSessionId)
                        .ifPresent(voteSessionService::syncRedisVotesToDb);
            } catch (Exception e) {
                log.error("투표 동기화 중 오류 발생: {}", key, e);
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
            session.complete();

            voteSessionService.syncRedisVotesToDb(session);

            voteRedisManager.deleteVoteCounts(session.getId());
            log.info("투표 세션 자동 종료 완료: {}", session.getId());
        }
    }
}