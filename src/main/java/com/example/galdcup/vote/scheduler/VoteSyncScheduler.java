package com.example.galdcup.vote.scheduler;

import com.example.galdcup.vote.VoteOptionRepository;
import com.example.galdcup.vote.VoteSession;
import com.example.galdcup.vote.VoteSessionRepository;
import com.example.galdcup.vote.VoteSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class VoteSyncScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteSessionRepository voteSessionRepository;
    private final VoteSessionService voteSessionService;

    /**
     * 1분마다 Redis 투표수를 DB에 반영
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncVotesToDb() {
        Set<String> keys = redisTemplate.keys("vote:*");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            try {
                // key: vote:{sessionId}:{optionId}
                String[] parts = key.split(":");
                if (parts.length < 3) continue;

                Long optionId = Long.valueOf(parts[2]);
                String value = redisTemplate.opsForValue().get(key);

                if (value != null) {
                    long count = Long.parseLong(value);

                    voteOptionRepository.incrementVoteCount(optionId, count);

                    redisTemplate.delete(key);
                }
            } catch (Exception e) {
                System.err.println("투표 동기화 실패: " + key + " - " + e.getMessage());
            }
        }
    }

    /**
     * 1분마다 투표 기간이 끝난 세션 종료
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void finishVoteSession() {
        List<VoteSession> endedSessions =
                voteSessionRepository.findByEndTimeBeforeAndIsFinishedFalse(OffsetDateTime.now());

        for (VoteSession voteSession : endedSessions) {
            voteSessionService.finishVoteSession(voteSession.getId());

            voteSession.setFinished(true);
        }
    }
}
