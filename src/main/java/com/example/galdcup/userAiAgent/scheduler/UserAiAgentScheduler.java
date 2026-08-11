package com.example.galdcup.userAiAgent.scheduler;

import com.example.galdcup.userAiAgent.UserAiAgentExecutionService;
import com.example.galdcup.userAiAgent.domain.UserAiAgent;
import com.example.galdcup.userAiAgent.domain.UserAiAgentRepository;
import com.example.galdcup.userAiAgent.redis.UserAiAgentRedisManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAiAgentScheduler {

    private final UserAiAgentRepository userAiAgentRepository;
    private final UserAiAgentRedisManager redisManager;
    private final UserAiAgentExecutionService executionService;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 10초마다 주기적으로 확인하여 대기 시간이 끝난 활성 AI 봇을 실행합니다.
     */
    @Scheduled(fixedDelay = 10000)
    public void scheduleAiAgents() {
        OffsetDateTime now = OffsetDateTime.now(KST_ZONE);

        // 1. 활성 상태이면서 만료 시간이 지나지 않은 봇들을 Fetch Join으로 조회
        List<UserAiAgent> activeAgents = userAiAgentRepository.findAllActiveAgentsWithTarget(now);

        for (UserAiAgent agent : activeAgents) {
            Long agentId = agent.getId();

            // 2. Redis에 쿨타임 키가 활성화되어 있는지 확인
            if (!redisManager.isCooldownActive(agentId)) {
                log.info("[AI 용병 스케줄러] 봇 작전 수행 시작 - Agent ID: {}", agentId);

                try {
                    // 3. 글 또는 댓글 작성 실행
                    executionService.execute(agent);
                    log.info("[AI 용병 스케줄러] 봇 작전 수행 완료 - Agent ID: {}", agentId);
                } catch (Exception e) {
                    log.error("[AI 용병 스케줄러] 봇 작전 수행 실패 - Agent ID: {}, 사유: {}", agentId, e.getMessage());
                }
            }
        }
    }
}