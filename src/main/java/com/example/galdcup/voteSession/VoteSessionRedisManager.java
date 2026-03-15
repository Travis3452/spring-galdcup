package com.example.galdcup.voteSession;

import com.example.galdcup.common.CachedPageResponse;
import com.example.galdcup.voteSession.dto.VoteSessionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class VoteSessionRedisManager {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String VOTE_SESSION_META_PREFIX = "galdcup:vote-sessions:meta:";
    private static final String VOTE_COUNT_KEY_PREFIX = "galdcup:vote-sessions:count:";
    private static final String PAST_SESSIONS_PREFIX = "galdcup:vote-sessions:past:";

    public Optional<VoteSessionDto> getActiveVoteSession(Long boardId) {
        String key = VOTE_SESSION_META_PREFIX + boardId;
        return Optional.ofNullable((VoteSessionDto) redisTemplate.opsForValue().get(key));
    }

    @Async
    public void saveVoteSession(Long boardId, VoteSessionDto voteSessionDto) {
        String key = VOTE_SESSION_META_PREFIX + boardId;
        redisTemplate.opsForValue().set(key, voteSessionDto, Duration.ofHours(1));
    }

    @Async
    public void deleteVoteSession(Long boardId) {
        redisTemplate.delete(VOTE_SESSION_META_PREFIX + boardId);
    }

    public Map<Object, Object> getVoteCounts(Long voteSessionId) {
        String key = VOTE_COUNT_KEY_PREFIX + voteSessionId;
        return redisTemplate.opsForHash().entries(key);
    }

    @Async
    public void incrementVoteCount(Long voteSessionId, int optionIndex) {
        String key = VOTE_COUNT_KEY_PREFIX + voteSessionId;
        redisTemplate.opsForHash().increment(key, String.valueOf(optionIndex), 1);
    }

    @Async
    public void deleteVoteCounts(Long voteSessionId) {
        redisTemplate.delete(VOTE_COUNT_KEY_PREFIX + voteSessionId);
    }

    public Optional<CachedPageResponse<VoteSessionDto>> getPastVoteSessions(Long boardId, int page, int size) {
        String key = generatePastKey(boardId, page, size);
        return Optional.ofNullable((CachedPageResponse<VoteSessionDto>) redisTemplate.opsForValue().get(key));
    }

    @Async
    public void savePastVoteSessions(Long boardId, int page, int size, CachedPageResponse<VoteSessionDto> data) {
        String key = generatePastKey(boardId, page, size);
        redisTemplate.opsForValue().set(key, data, Duration.ofDays(1));
    }

    @Async
    public void deletePastVoteSessions(Long boardId) {
        String pattern = PAST_SESSIONS_PREFIX + boardId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.unlink(keys);
        }
    }

    private String generatePastKey(Long boardId, int page, int size) {
        return PAST_SESSIONS_PREFIX + boardId + ":" + page + ":" + size;
    }
}