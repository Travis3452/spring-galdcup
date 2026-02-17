package com.example.galdcup.vote.scheduler;

import com.example.galdcup.vote.VoteOptionRepository;
import com.example.galdcup.voteSession.VoteSession;
import com.example.galdcup.voteSession.VoteSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class VoteSyncScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteSessionRepository voteSessionRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncVotesToDb() {
        Set<String> keys = redisTemplate.keys("voteSession:count:*");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            Long voteSessionId = Long.valueOf(key.split(":")[2]);
            VoteSession session = voteSessionRepository.findById(voteSessionId).orElse(null);
            if (session == null) continue;

            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

            entries.forEach((optionIndex, count) -> {
                int selectedOptionIndex = Integer.parseInt(optionIndex.toString());
                long voteCount = Long.parseLong(count.toString());

                if (selectedOptionIndex < session.getOptions().size()) {
                    Long optionId = session.getOptions().get(selectedOptionIndex).getId();
                    voteOptionRepository.incrementVoteCount(optionId, voteCount);
                }
            });

            redisTemplate.delete(key);
        }
    }

    /**
     * 종료된 세션 처리
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void finishVoteSession() {
        List<VoteSession> endedSessions =
                voteSessionRepository.findByEndTimeBeforeAndIsFinishedFalse(OffsetDateTime.now());

        for (VoteSession voteSession : endedSessions) {
            voteSession.setFinished(true);
        }
    }
}