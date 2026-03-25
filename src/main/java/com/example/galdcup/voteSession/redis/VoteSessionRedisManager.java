package com.example.galdcup.voteSession.redis;

import com.example.galdcup.common.redis.CachedPageResponse;
import com.example.galdcup.voteSession.response.VoteSessionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * 투표 세션 데이터 캐싱 관리 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class VoteSessionRedisManager {

    private final RedisTemplate<String, Object> redisTemplate;

    /** Redis 키 구분자 */
    private static final String VOTE_SESSION_META_PREFIX = "galdcup:vote-sessions:meta:";
    private static final String PAST_SESSIONS_PREFIX = "galdcup:vote-sessions:past:";

    /** 특정 게시판의 현재 진행 중인 투표 세션 조회 */
    public Optional<VoteSessionDto> getActiveVoteSession(Long boardId) {
        String key = VOTE_SESSION_META_PREFIX + boardId;
        return Optional.ofNullable((VoteSessionDto) redisTemplate.opsForValue().get(key));
    }

    /** 진행 중인 투표 세션 정보를 1시간 동안 캐시에 저장 */
    public void saveVoteSession(Long boardId, VoteSessionDto voteSessionDto) {
        String key = VOTE_SESSION_META_PREFIX + boardId;
        redisTemplate.opsForValue().set(key, voteSessionDto, Duration.ofHours(1));
    }

    /** 특정 게시판의 진행 중인 투표 세션 캐시 무효화 */
    public void deleteVoteSession(Long boardId) {
        redisTemplate.unlink(VOTE_SESSION_META_PREFIX + boardId);
    }

    /** 특정 게시판의 과거 투표 이력 페이징 캐시 조회 */
    public Optional<CachedPageResponse<VoteSessionDto>> getPastVoteSessions(Long boardId, int page, int size) {
        String key = generatePastKey(boardId, page, size);
        return Optional.ofNullable((CachedPageResponse<VoteSessionDto>) redisTemplate.opsForValue().get(key));
    }

    /** 과거 투표 이력 페이징 데이터를 1일 동안 캐시에 저장 */
    public void savePastVoteSessions(Long boardId, int page, int size, CachedPageResponse<VoteSessionDto> data) {
        String key = generatePastKey(boardId, page, size);
        redisTemplate.opsForValue().set(key, data, Duration.ofDays(1));
    }

    /** 특정 게시판과 관련된 모든 페이지의 과거 이력 캐시 일괄 삭제 */
    public void deletePastVoteSessions(Long boardId) {
        String pattern = PAST_SESSIONS_PREFIX + boardId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            // 여러 개의 키를 한 번에 비차단 방식으로 삭제 처리
            redisTemplate.unlink(keys);
        }
    }

    /** 게시판 ID와 페이징 정보를 조합한 과거 이력용 고유 키 생성 */
    private String generatePastKey(Long boardId, int page, int size) {
        return PAST_SESSIONS_PREFIX + boardId + ":" + page + ":" + size;
    }
}