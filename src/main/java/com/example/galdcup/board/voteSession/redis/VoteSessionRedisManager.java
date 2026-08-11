package com.example.galdcup.board.voteSession.redis;

import com.example.galdcup.common.redis.CachedPageResponse;
import com.example.galdcup.gemini.response.OpinionAnalysisResponse;
import com.example.galdcup.board.voteSession.response.VoteSessionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 투표 세션 데이터 캐싱 관리 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class VoteSessionRedisManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_ACTIVE = "galdcup:vote:session:%d:latest";
    private static final String KEY_PAST_PAGE = "galdcup:vote:session:%d:past:v:%s:p:%d:s:%d";
    private static final String KEY_PAST_VERSION = "galdcup:vote:session:%d:past:version";
    private static final String KEY_ANALYSIS = "galdcup:vote:session:%d:analysis";

    /** 현재 게시판의 과거 이력 캐시 버전 조회 */
    public String getPastVersion(Long boardId) {
        String version = stringRedisTemplate.opsForValue().get(String.format(KEY_PAST_VERSION, boardId));
        return (version != null) ? version : "0";
    }

    /** 전체 페이지 캐시 조회 */
    public Optional<CachedPageResponse<VoteSessionDto>> getPastVoteSessions(Long boardId, int page, int size) {
        String version = getPastVersion(boardId);
        String key = String.format(KEY_PAST_PAGE, boardId, version, page, size);
        return Optional.ofNullable((CachedPageResponse<VoteSessionDto>) redisTemplate.opsForValue().get(key));
    }

    /** 전체 페이지 캐시 저장 */
    public void savePastVoteSessions(Long boardId, int pageNumber, int pageSize, CachedPageResponse<VoteSessionDto> data) {
        String version = getPastVersion(boardId);
        String key = String.format(KEY_PAST_PAGE, boardId, version, pageNumber, pageSize);
        redisTemplate.opsForValue().set(key, data, Duration.ofDays(1));
    }

    /** 버전 번호를 올려서 기존 모든 페이지 캐시를 무효화 */
    public void deletePastVoteSessions(Long boardId) {
        stringRedisTemplate.opsForValue().increment(String.format(KEY_PAST_VERSION, boardId));
    }

    public Optional<VoteSessionDto> getLatestVoteSession(Long boardId) {
        return Optional.ofNullable((VoteSessionDto) redisTemplate.opsForValue().get(String.format(KEY_ACTIVE, boardId)));
    }

    public void saveLatestVoteSession(Long boardId, VoteSessionDto dto) {
        redisTemplate.opsForValue().set(String.format(KEY_ACTIVE, boardId), dto, Duration.ofHours(1));
    }

    public void deleteLatestVoteSession(Long boardId) {
        redisTemplate.unlink(String.format(KEY_ACTIVE, boardId));
    }

    /** 여론 분석 결과 조회 */
    public Optional<OpinionAnalysisResponse> getOpinionAnalysis(Long sessionId) {
        String key = String.format(KEY_ANALYSIS, sessionId);
        return Optional.ofNullable((OpinionAnalysisResponse) redisTemplate.opsForValue().get(key));
    }

    /** 여론 분석 결과 저장 (TTL: 1일) */
    public void saveOpinionAnalysis(Long sessionId, OpinionAnalysisResponse response) {
        String key = String.format(KEY_ANALYSIS, sessionId);
        redisTemplate.opsForValue().set(key, response, Duration.ofDays(1));
    }

    /** 여론 분석 결과 캐시 삭제 */
    public void deleteOpinionAnalysis(Long sessionId) {
        redisTemplate.unlink(String.format(KEY_ANALYSIS, sessionId));
    }
}