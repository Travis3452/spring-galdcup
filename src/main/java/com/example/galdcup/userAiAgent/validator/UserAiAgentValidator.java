package com.example.galdcup.userAiAgent.validator;

import com.example.galdcup.userAiAgent.domain.UserAiAgent;
import com.example.galdcup.userAiAgent.domain.UserAiAgentRepository;
import com.example.galdcup.userAiAgent.gemini.UserAiAgentGeminiClient;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * UserAiAgent 도메인 검증기
 */
@Component
@RequiredArgsConstructor
public class UserAiAgentValidator {

    private final UserAiAgentRepository userAiAgentRepository;
    private final UserAiAgentGeminiClient geminiClient;

    /**
     * ID로 AI 용병 조회 (없을 경우 예외 발생)
     */
    public UserAiAgent findByIdOrThrow(Long id) {
        return userAiAgentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 AI 용병입니다. ID: " + id));
    }

    /**
     * 유저 ID로 AI 용병 조회 (없을 경우 예외 발생)
     */
    public UserAiAgent findByUserIdOrThrow(Long userId) {
        return userAiAgentRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("등록된 AI 용병이 없습니다. UserId: " + userId));
    }

    /**
     * 이미 활성화된 AI 용병이 존재하는지 검증 (유저당 1대 제한)
     */
    public void validateAlreadyExists(Long userId) {
        if (userAiAgentRepository.existsByUserId(userId)) {
            throw new IllegalStateException("이미 등록된 AI 용병이 존재합니다. 유저당 1대만 생성할 수 있습니다.");
        }
    }

    /**
     * 사용자가 제공한 Gemini API Key의 유효성 검증
     */
    public void validateApiKey(String rawApiKey) {
        geminiClient.validateApiKey(rawApiKey);
    }

    /**
     * 본인의 AI 용병인지 소유권 검증
     */
    public void validateOwnership(UserAiAgent agent, Long currentUserId) {
        if (!agent.getUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("본인의 AI 용병에 대해서만 변경 또는 삭제가 가능합니다.");
        }
    }
}