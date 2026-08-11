package com.example.galdcup.userAiAgent.response;

import com.example.galdcup.userAiAgent.domain.AgentMode;
import com.example.galdcup.userAiAgent.domain.UserAiAgent;

import java.time.OffsetDateTime;

public record UserAiAgentDto(
        Long id,
        Long userId,
        Long targetBoardId,
        String targetBoardName,
        AgentMode agentMode,
        String personaPrompt,
        boolean isActive,
        int intervalMinutes,
        OffsetDateTime lastExecutedAt,
        OffsetDateTime expiredAt
) {
    public static UserAiAgentDto from(UserAiAgent agent) {
        return new UserAiAgentDto(
                agent.getId(),
                agent.getUser().getId(),
                agent.getTargetBoard().getId(),
                agent.getTargetBoard().getTopic(),
                agent.getAgentMode(),
                agent.getPersonaPrompt(),
                agent.isActive(),
                agent.getIntervalMinutes(),
                agent.getLastExecutedAt(),
                agent.getExpiredAt()
        );
    }
}