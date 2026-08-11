package com.example.galdcup.userAiAgent;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.common.util.ApiKeyEncryptor;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.validator.UserValidator;
import com.example.galdcup.userAiAgent.domain.UserAiAgent;
import com.example.galdcup.userAiAgent.domain.UserAiAgentRepository;
import com.example.galdcup.userAiAgent.redis.UserAiAgentRedisManager;
import com.example.galdcup.userAiAgent.request.CreateUserAiAgentRequest;
import com.example.galdcup.userAiAgent.response.UserAiAgentDto;
import com.example.galdcup.userAiAgent.validator.UserAiAgentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAiAgentService {

    private final UserAiAgentRepository userAiAgentRepository;
    private final UserValidator userValidator;
    private final BoardValidator boardValidator;
    private final UserAiAgentValidator userAiAgentValidator;
    private final ApiKeyEncryptor apiKeyEncryptor;
    private final UserAiAgentRedisManager userAiAgentRedisManager;

    /**
     * AI 용병 생성
     */
    @Transactional
    public UserAiAgentDto create(Long userId, CreateUserAiAgentRequest request) {
        userAiAgentValidator.validateAlreadyExists(userId);
        userAiAgentValidator.validateApiKey(request.apiKey());

        User user = userValidator.findByIdOrThrow(userId);
        Board targetBoard = boardValidator.findByIdOrThrow(request.targetBoardId());

        String encryptedApiKey = apiKeyEncryptor.encrypt(request.apiKey());

        UserAiAgent agent = UserAiAgent.create(
                user,
                targetBoard,
                request.agentMode(),
                encryptedApiKey,
                request.personaPrompt()
        );

        UserAiAgent savedAgent = userAiAgentRepository.save(agent);
        return UserAiAgentDto.from(savedAgent);
    }

    /**
     * 로그인한 유저의 AI 용병 조회
     */
    public UserAiAgentDto findMyAgent(Long userId) {
        UserAiAgent agent = userAiAgentValidator.findByUserIdOrThrow(userId);
        return UserAiAgentDto.from(agent);
    }

    /**
     * 내 AI 용병 삭제
     */
    @Transactional
    public void deleteMyAgent(Long userId) {
        UserAiAgent agent = userAiAgentValidator.findByUserIdOrThrow(userId);

        userAiAgentRedisManager.deleteCooldown(agent.getId());

        userAiAgentRepository.delete(agent);
    }
}