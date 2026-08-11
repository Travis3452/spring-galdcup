package com.example.galdcup.board.vote.redis;

import com.example.galdcup.board.vote.domain.Vote;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 실시간 투표 집계 및 중복 투표 방지를 위한 Redis 관리 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class VoteRedisManager {

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> voteScript;

    private static final String USER_VOTE_KEY = "galdcup:vote:session:%d:user:%d";
    private static final String VOTE_COUNT_KEY = "galdcup:vote:session:%d:counts";
    private static final String DIRTY_SESSIONS_KEY = "galdcup:vote:dirty-sessions";

    @PostConstruct
    public void init() {
        voteScript = new DefaultRedisScript<>();
        voteScript.setLocation(new ClassPathResource("scripts/vote.lua"));
        voteScript.setResultType(Long.class);
    }

    /**
     * 투표 실행 및 전체 카운트 증가
     */
    public void castVote(Vote vote) {
        String userKey = String.format(USER_VOTE_KEY, vote.getVoteSessionId(), vote.getUserId());
        String countKey = String.format(VOTE_COUNT_KEY, vote.getVoteSessionId());

        Long result = redisTemplate.execute(
                voteScript,
                List.of(userKey, countKey, DIRTY_SESSIONS_KEY),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(vote.getTtl()),
                String.valueOf(vote.getSelectedOptionIndex()),
                String.valueOf(vote.getVoteSessionId())
        );

        if (result == null || result == 0) {
            throw new IllegalStateException("이미 해당 게시판에 투표하였습니다.");
        }
    }

    /** Redis에 데이터 존재 여부 확인 */
    public boolean hasVoteCounts(Long voteSessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(String.format(VOTE_COUNT_KEY, voteSessionId)));
    }

    /** DB 데이터를 Redis로 초기 로드 */
    public void warmUpVoteCounts(Long voteSessionId, Map<String, String> initialCounts, long ttl) {
        String countKey = String.format(VOTE_COUNT_KEY, voteSessionId);
        redisTemplate.opsForHash().putAll(countKey, initialCounts);
        redisTemplate.expire(countKey, ttl, TimeUnit.SECONDS);
    }

    /** 실시간 전체 득표수 조회 */
    public Map<String, String> getVoteCounts(Long voteSessionId) {
        String countKey = String.format(VOTE_COUNT_KEY, voteSessionId);
        Map<Object, Object> results = redisTemplate.opsForHash().entries(countKey);

        return results.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().toString()
                ));
    }

    /** 실시간 총 투표 수 조회 */
    public long getTotalVoteCount(Long voteSessionId) {
        String countKey = String.format(VOTE_COUNT_KEY, voteSessionId);
        List<Object> values = redisTemplate.opsForHash().values(countKey);

        return values.stream()
                .mapToLong(val -> Long.parseLong(val.toString()))
                .sum();
    }

    /** 메모리 정리 */
    public void deleteVoteCounts(Long voteSessionId) {
        redisTemplate.unlink(String.format(VOTE_COUNT_KEY, voteSessionId));
    }
}