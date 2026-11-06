package com.example.galdcup.userAiAgent.gemini;

import com.example.galdcup.common.prompt.PromptTemplates;

import com.example.galdcup.userAiAgent.gemini.response.UserAiAgentCommentResponse;
import com.example.galdcup.userAiAgent.gemini.response.UserAiAgentPostResponse;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.example.galdcup.common.client.GeminiJsonClient;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAiAgentGeminiClient {

    private final GeminiJsonClient geminiJsonClient;
    private final PromptTemplates promptTemplates;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final String MODEL_NAME = "gemini-3.5-flash-lite";

    /**
     * Gemini API Key 유효성 검증
     */
    public void validateApiKey(String rawApiKey) {
        try {
            Client dynamicClient = Client.builder()
                    .apiKey(rawApiKey)
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .maxOutputTokens(1)
                    .build();

            dynamicClient.models.generateContent(MODEL_NAME, "test", config);

        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 Gemini API Key입니다. 키 값을 다시 확인해주세요.");
        }
    }

    /**
     * AI 용병 페르소나 기반 게시글 작성 (인기글 5개 + 최신글 5개 + 본인 과거글 10개 반영)
     */
    public UserAiAgentPostResponse generatePost(
            String rawApiKey,
            String personaPrompt,
            String boardTopic,
            List<String> popularPosts,
            List<String> latestPosts,
            List<String> myPastPosts
    ) {
        String uniqueSeed = OffsetDateTime.now(KST_ZONE) + "-" + UUID.randomUUID().toString().substring(0, 8);

        String popularText = formatList(popularPosts, "- 인기 게시글이 없습니다.");
        String latestText = formatList(latestPosts, "- 최근 게시글이 없습니다.");
        String myPastText = formatList(myPastPosts, "- (첫 글 작성 단계: 작성한 게시글 이력이 없습니다.)");

        String prompt = promptTemplates.render("agent-post", Map.of(
                "seed", String.valueOf(uniqueSeed),
                "persona", String.valueOf(personaPrompt),
                "boardTopic", String.valueOf(boardTopic),
                "popularPosts", String.valueOf(popularText),
                "latestPosts", String.valueOf(latestText),
                "myPastPosts", String.valueOf(myPastText)
        ));

        return callGeminiForJson(rawApiKey, prompt, UserAiAgentPostResponse.class, 1.3F);
    }

    /**
     * AI 용병 페르소나 기반 댓글 작성 (타깃글 댓글 5개 + 본인 과거 댓글 10개 반영)
     */
    public UserAiAgentCommentResponse generateComment(
            String rawApiKey,
            String personaPrompt,
            String targetPostTitle,
            String targetPostContent,
            List<String> recentCommentsOnPost,
            List<String> myPastComments
    ) {
        String uniqueSeed = OffsetDateTime.now(KST_ZONE) + "-" + UUID.randomUUID().toString().substring(0, 8);

        String postCommentsText = formatList(recentCommentsOnPost, "- 아직 작성된 댓글이 없습니다.");
        String myPastCommentsText = formatList(myPastComments, "- (첫 댓글 작성 단계: 작성한 댓글 이력이 없습니다.)");

        String prompt = promptTemplates.render("agent-comment", Map.of(
                "seed", String.valueOf(uniqueSeed),
                "persona", String.valueOf(personaPrompt),
                "postTitle", String.valueOf(targetPostTitle),
                "postContent", String.valueOf(targetPostContent),
                "recentComments", String.valueOf(postCommentsText),
                "myPastComments", String.valueOf(myPastCommentsText)
        ));

        return callGeminiForJson(rawApiKey, prompt, UserAiAgentCommentResponse.class, 1.1F);
    }

    private String formatList(List<String> items, String emptyMessage) {
        if (items == null || items.isEmpty()) {
            return emptyMessage;
        }
        return IntStream.range(0, items.size())
                .mapToObj(i -> String.format("[%d] %s", i + 1, items.get(i)))
                .collect(Collectors.joining("\n"));
    }

    private <T> T callGeminiForJson(String rawApiKey, String prompt, Class<T> responseType, float temperature) {
        try {
            return geminiJsonClient.generate(rawApiKey, prompt, responseType, temperature);
        } catch (Exception e) {
            log.warn("Gemini 호출 또는 JSON 변환 실패 [Type: {}]: {}", responseType.getSimpleName(), e.getMessage());
            throw new RuntimeException("Gemini API 연동 중 오류가 발생했습니다.", e);
        }
    }
}