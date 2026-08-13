package com.example.galdcup.userAiAgent;

import com.example.galdcup.common.util.ApiKeyEncryptor;
import com.example.galdcup.userAiAgent.domain.AgentMode;
import com.example.galdcup.userAiAgent.domain.UserAiAgent;
import com.example.galdcup.userAiAgent.redis.UserAiAgentRedisManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAiAgentExecutionService {

    private final ApiKeyEncryptor apiKeyEncryptor;
    private final UserAiAgentRedisManager redisManager;
    private final UserAiAgentExecutor agentExecutor;

    public void execute(UserAiAgent agent) {
        Long agentId = agent.getId();

        try {
            String rawApiKey = apiKeyEncryptor.decrypt(agent.getEncryptedApiKey());

            if (agent.getAgentMode() == AgentMode.POST) {
                agentExecutor.executePostCreation(agent, rawApiKey);
            } else if (agent.getAgentMode() == AgentMode.COMMENT) {
                agentExecutor.executeCommentCreation(agent, rawApiKey);
            }

            long ttlSeconds = (long) agent.getIntervalMinutes() * 60;
            redisManager.setCooldown(agentId, ttlSeconds);

        } catch (Exception e) {
            log.error("AI 용병 실행 실패 (agentId: {}, mode: {}): {}",
                    agentId, agent.getAgentMode(), e.getMessage(), e);

            redisManager.setCooldown(agentId, 300L);
        }
    }
}