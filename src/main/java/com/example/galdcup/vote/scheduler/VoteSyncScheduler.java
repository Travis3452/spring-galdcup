package com.example.galdcup.vote.scheduler;

import com.example.galdcup.vote.VoteOptionRepository;
import com.example.galdcup.vote.VoteRedisManager;
import com.example.galdcup.voteSession.VoteSession;
import com.example.galdcup.voteSession.VoteSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteSyncScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteSessionRepository voteSessionRepository;
    private final VoteRedisManager voteRedisManager;

    /**
     * Redis의 전체 투표수를 DB에 덮어쓰기 방식으로 동기화
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncVotesToDb() {
        Set<String> keys = redisTemplate.keys("voteSession:count:*");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            try {
                Long voteSessionId = Long.valueOf(key.split(":")[2]);
                VoteSession session = voteSessionRepository.findById(voteSessionId).orElse(null);
                if (session == null) continue;

                Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

                entries.forEach((optionIndex, count) -> {
                    int selectedOptionIndex = Integer.parseInt(optionIndex.toString());
                    long totalVoteCount = Long.parseLong(count.toString());

                    if (selectedOptionIndex < session.getOptions().size()) {
                        Long optionId = session.getOptions().get(selectedOptionIndex).getId();

                        voteOptionRepository.updateVoteCount(optionId, totalVoteCount);
                    }
                });

                log.debug("투표 동기화 완료 (덮어쓰기): Session {}", voteSessionId);

            } catch (Exception e) {
                log.error("투표 동기화 중 오류 발생: {}", key, e);
            }
        }
    }

    /**
     * 종료된 세션 처리 및 메모리 정리
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void finishVoteSession() {
        List<VoteSession> endedSessions =
                voteSessionRepository.findByEndTimeBeforeAndIsFinishedFalse(OffsetDateTime.now());

        for (VoteSession voteSession : endedSessions) {
            voteSession.setFinished(true);
            syncSingleSession(voteSession);
            voteRedisManager.deleteVoteCounts(voteSession.getId());

            log.info("투표 세션 종료 및 Redis 데이터 정리 완료: {}", voteSession.getId());
        }
    }

    private void syncSingleSession(VoteSession session) {
        String key = "voteSession:count:" + session.getId();
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        entries.forEach((optionIndex, count) -> {
            int idx = Integer.parseInt(optionIndex.toString());
            long total = Long.parseLong(count.toString());
            if (idx < session.getOptions().size()) {
                voteOptionRepository.updateVoteCount(session.getOptions().get(idx).getId(), total);
            }
        });
    }
}