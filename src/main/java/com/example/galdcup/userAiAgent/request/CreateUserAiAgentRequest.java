package com.example.galdcup.userAiAgent.request;

import com.example.galdcup.userAiAgent.domain.AgentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserAiAgentRequest(
        @NotNull(message = "AI 용병이 활동할 게시판을 선택해야 합니다.")
        Long targetBoardId,

        @NotNull(message = "AI 용병의 작동 모드를 선택해야 합니다.")
        AgentMode agentMode,

        @NotBlank(message = "Gemini API Key를 입력해야 합니다.")
        String apiKey,

        @NotBlank(message = "페르소나 프롬프트를 500자 이하로 입력하세요.")
        @Size(max = 500, message = "페르소나 프롬프트를 500자 이하로 입력하세요.")
        String personaPrompt
) {}