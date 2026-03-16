package com.example.galdcup.vote;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class VoteRedisManager {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_VOTE_KEY_PREFIX = "galdcup:vote-sessions:%d:user:%d";
    private static final String VOTE_COUNT_KEY_PREFIX = "voteSession:count:%d";
    private static final String VOTE_COUNT_KEY_PATTERN = "voteSession:count:*";

    /**
     * 투표 실행 및 카운트
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
     * 특정 투표 세션의 실시간 득표수 조회 (WebSocket 브로드캐스트 등에서 사용)
     */
    public Map<Object, Object> getVoteCounts(Long voteSessionId) {
        String countKey = String.format(VOTE_COUNT_KEY_PREFIX, voteSessionId);
        return redisTemplate.opsForHash().entries(countKey);
    }

    /**
     * 투표 세션이 완전히 삭제되거나 강제 초기화될 때 카운트 비동기 삭제 (unlink)
     */
    public void deleteVoteCounts(Long voteSessionId) {
        String countKey = String.format(VOTE_COUNT_KEY_PREFIX, voteSessionId);
        redisTemplate.unlink(countKey);
    }

    /**
     * DB 동기화를 위한 모든 세션의 카운트 키 목록 조회
     */
    public Set<String> getAllVoteCountKeys() {
        return redisTemplate.keys(VOTE_COUNT_KEY_PATTERN);
    }

    /**
     * 스케줄러에서 특정 키의 해시(투표 항목별 득표수) 데이터를 읽어올 때 사용
     */
    public Map<Object, Object> getVoteCountEntries(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * DB 동기화가 끝난 카운트 데이터를 메모리에서 비동기 삭제 (성능 최적화)
     */
    public void unlinkKey(String key) {
        redisTemplate.unlink(key);
    }
}