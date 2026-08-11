package com.example.galdcup.userAiAgent.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 용병의 Redis 쿨타임 및 락 생명주기를 관리하는 매니저.
 */
@Component
@RequiredArgsConstructor
public class UserAiAgentRedisManager {

    private final RedisTemplate<String, String> redisTemplate;

    /** Redis 키 구분자 */
    private static final String COOLDOWN_PREFIX = "galdcup:ai-agent:cooldown:";

    /**
     * 특정 AI 용병의 쿨타임이 활성화 상태인지 확인합니다.
     */
    public boolean isCooldownActive(Long agentId) {
        Boolean hasKey = redisTemplate.hasKey(COOLDOWN_PREFIX + agentId);
        return Boolean.TRUE.equals(hasKey);
    }

    /**
     * AI 용병의 쿨타임을 등록합니다.
     *
     * @param agentId AI 용병 ID
     * @param ttlSeconds 쿨타임 유지 시간 (초)
     */
    public void setCooldown(Long agentId, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                COOLDOWN_PREFIX + agentId,
                "active",
                Duration.ofSeconds(ttlSeconds)
        );
    }

    /**
     * AI 용병의 쿨타임 및 상태 키를 즉시 삭제합니다. (용병 삭제 또는 예외 발생 시)
     */
    public void deleteCooldown(Long agentId) {
        redisTemplate.delete(COOLDOWN_PREFIX + agentId);
    }
}