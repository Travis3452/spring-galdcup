package com.example.galdcup.vote.redis;

import com.example.galdcup.vote.domain.Vote;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class VoteRedisManager {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_VOTE_KEY_PREFIX = "galdcup:vote-sessions:%d:user:%d";
    private static final String VOTE_COUNT_KEY_PREFIX = "voteSession:count:%d";

    /**
     * 투표 실행 및 전체 카운트 증가
     */
    public void castVote(Vote vote) {
        String userKey = String.format(USER_VOTE_KEY_PREFIX, vote.getVoteSessionId(), vote.getUserId());
        Boolean isFirstVote = redisTemplate.opsForValue()
                .setIfAbsent(userKey, vote, vote.getTtl(), TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(isFirstVote)) {
            throw new IllegalStateException("이미 해당 게시판에 투표하였습니다.");
        }

        String countKey = String.format(VOTE_COUNT_KEY_PREFIX, vote.getVoteSessionId());
        redisTemplate.opsForHash().increment(countKey, String.valueOf(vote.getSelectedOptionIndex()), 1);
    }

    /**
     * Redis에 해당 세션의 카운트 데이터가 존재하는지 확인
     */
    public boolean hasVoteCounts(Long voteSessionId) {
        String countKey = String.format(VOTE_COUNT_KEY_PREFIX, voteSessionId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(countKey));
    }

    /**
     * DB의 데이터를 Redis로 초기 로드
     */
    public void warmUpVoteCounts(Long voteSessionId, Map<String, String> initialCounts, long ttl) {
        String countKey = String.format(VOTE_COUNT_KEY_PREFIX, voteSessionId);
        redisTemplate.opsForHash().putAll(countKey, initialCounts);
        redisTemplate.expire(countKey, ttl, TimeUnit.SECONDS);
    }

    /**
     * 실시간 전체 득표수 조회
     */
    public Map<Object, Object> getVoteCounts(Long voteSessionId) {
        String countKey = String.format(VOTE_COUNT_KEY_PREFIX, voteSessionId);
        return redisTemplate.opsForHash().entries(countKey);
    }

    /**
     * 세션 종료 시나 삭제 시 메모리 정리
     */
    public void deleteVoteCounts(Long voteSessionId) {
        redisTemplate.unlink(String.format(VOTE_COUNT_KEY_PREFIX, voteSessionId));
    }
}