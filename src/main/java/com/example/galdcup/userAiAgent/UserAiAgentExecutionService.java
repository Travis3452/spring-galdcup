package com.example.galdcup.userAiAgent;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.comment.CommentService;
import com.example.galdcup.board.comment.request.CreateCommentRequest;
import com.example.galdcup.board.post.PostService;
import com.example.galdcup.board.post.domain.Post;
import com.example.galdcup.board.post.domain.PostRepository;
import com.example.galdcup.board.postCategory.domain.PostCategory;
import com.example.galdcup.board.postCategory.validator.PostCategoryValidator;
import com.example.galdcup.common.util.ApiKeyEncryptor;
import com.example.galdcup.userAiAgent.domain.AgentMode;
import com.example.galdcup.userAiAgent.domain.UserAiAgent;
import com.example.galdcup.userAiAgent.gemini.UserAiAgentGeminiClient;
import com.example.galdcup.userAiAgent.gemini.response.UserAiAgentCommentResponse;
import com.example.galdcup.userAiAgent.gemini.response.UserAiAgentPostResponse;
import com.example.galdcup.userAiAgent.redis.UserAiAgentRedisManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAiAgentExecutionService {

    private final ApiKeyEncryptor apiKeyEncryptor;
    private final UserAiAgentRedisManager redisManager;
    private final UserAiAgentGeminiClient geminiClient;
    private final PostService postService;
    private final CommentService commentService;
    private final PostRepository postRepository;
    private final PostCategoryValidator postCategoryValidator;

    public void execute(UserAiAgent agent) {
        Long agentId = agent.getId();

        try {
            String rawApiKey = apiKeyEncryptor.decrypt(agent.getEncryptedApiKey());

            if (agent.getAgentMode() == AgentMode.POST) {
                executePostCreation(agent, rawApiKey);
            } else if (agent.getAgentMode() == AgentMode.COMMENT) {
                executeCommentCreation(agent, rawApiKey);
            }

            long ttlSeconds = (long) agent.getIntervalMinutes() * 60;
            redisManager.setCooldown(agentId, ttlSeconds);

        } catch (Exception e) {
            redisManager.deleteCooldown(agentId);
        }
    }

    @Transactional
    protected void executePostCreation(UserAiAgent agent, String rawApiKey) {
        Board targetBoard = agent.getTargetBoard();
        PostCategory generalCategory = postCategoryValidator.findGeneralCategoryOrThrowByBoardId(targetBoard.getId());

        String latestTitle = postRepository.findTopByBoardIdOrderByCreatedAtDesc(targetBoard.getId())
                .map(Post::getTitle)
                .orElse("자유로운 이야기");

        UserAiAgentPostResponse postResponse = geminiClient.generatePost(
                rawApiKey,
                agent.getPersonaPrompt(),
                targetBoard.getTopic(),
                latestTitle
        );

        postService.create(
                targetBoard.getId(),
                generalCategory.getId(),
                agent.getUser().getId(),
                postResponse.title(),
                postResponse.content()
        );

        agent.updateLastExecutedAt();
    }

    @Transactional
    protected void executeCommentCreation(UserAiAgent agent, String rawApiKey) {
        Board targetBoard = agent.getTargetBoard();

        Post targetPost = postRepository.findTopByBoardIdOrderByCreatedAtDesc(targetBoard.getId())
                .orElseThrow(() -> new IllegalStateException("댓글을 작성할 최신 게시글이 없습니다."));

        UserAiAgentCommentResponse commentResponse = geminiClient.generateComment(
                rawApiKey,
                agent.getPersonaPrompt(),
                targetPost.getTitle(),
                targetPost.getContent()
        );

        CreateCommentRequest request = new CreateCommentRequest(null, commentResponse.content());
        commentService.create(targetPost.getId(), agent.getUser().getId(), request);

        agent.updateLastExecutedAt();
    }
}