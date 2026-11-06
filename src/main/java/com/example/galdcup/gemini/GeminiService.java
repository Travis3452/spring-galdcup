package com.example.galdcup.gemini;

import com.example.galdcup.common.prompt.PromptTemplates;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.gemini.response.OpinionAnalysisResponse;
import com.example.galdcup.gemini.response.VoteSessionContextResponse;
import com.example.galdcup.common.client.GeminiJsonClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 생성형 AI(Gemini)를 활용한 투표 세션 자동 생성 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final GeminiJsonClient geminiJsonClient;
    private final PromptTemplates promptTemplates;
    private final BoardValidator boardValidator;

    /**
     * 게시판 주제와 설명을 바탕으로 AI가 기획한 갈드컵 투표 세션 추천
     */
    public VoteSessionContextResponse getRecommendation(Long boardId) {
        Board board = boardValidator.findByIdOrThrow(boardId);

        String prompt = promptTemplates.render("vote-recommendation", Map.of(
                "seed", String.valueOf(LocalDateTime.now()),
                "boardTopic", String.valueOf(board.getTopic()),
                "boardDescription", String.valueOf(board.getDescription())
        ));

        return callGeminiForJson(prompt, VoteSessionContextResponse.class, 0.8F);
    }

    /**
     * 수집된 여론(댓글)을 분석하여 후보별 예상 지지율 및 AI 분석평 산출
     */
    public OpinionAnalysisResponse analyzeOpinion(String topic, String description, List<String> candidates, String comments) {
        String prompt = promptTemplates.render("opinion-analysis", Map.of(
                "seed", String.valueOf(LocalDateTime.now()),
                "topic", String.valueOf(topic),
                "description", String.valueOf(description),
                "candidates", String.valueOf(String.join(", ", candidates)),
                "comments", String.valueOf(comments)
        ));

        return callGeminiForJson(prompt, OpinionAnalysisResponse.class, 0.3F);
    }

    /**
     * Gemini API 호출 및 JSON 파싱 처리
     */
    private <T> T callGeminiForJson(String prompt, Class<T> responseType, float temperature) {
        try {
            return geminiJsonClient.generate(apiKey, prompt, responseType, temperature);
        } catch (Exception e) {
            log.warn("Gemini 호출 또는 JSON 변환 오류 [Type: {}]: {}", responseType.getSimpleName(), e.getMessage());
            throw new RuntimeException("AI 서비스 통신 오류", e);
        }
    }
}
