package com.example.galdcup.gemini.opinionAnalysis.redis;

import com.example.galdcup.gemini.response.OpinionAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OpinionAnalysisRedisManager {

    private static final String KEY_ANALYSIS = "galdcup:vote:session:%d:analysis";

    private final RedisTemplate<String, Object> redisTemplate;

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