package com.example.galdcup.userAiAgent.gemini;

import com.example.galdcup.userAiAgent.gemini.response.UserAiAgentCommentResponse;
import com.example.galdcup.userAiAgent.gemini.response.UserAiAgentPostResponse;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAiAgentGeminiClient {

    private final ObjectMapper objectMapper;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final String MODEL_NAME = "gemini-3.5-flash-lite-preview";

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
     * AI 용병 페르소나 기반 게시글 작성 (랜덤성 및 다양성 강화)
     */
    public UserAiAgentPostResponse generatePost(String rawApiKey, String personaPrompt, String boardTopic, String referenceTitle) {
        String uniqueSeed = OffsetDateTime.now(KST_ZONE) + "-" + UUID.randomUUID().toString().substring(0, 8);

        String prompt = String.format("""
            ### [Unique Generation Seed: %s]
            당신은 온라인 커뮤니티 유저입니다. 아래 게시판 주제와 무작위로 참고할 과거 글 제목을 바탕으로, 완전히 새롭고 독창적인 게시글 1개를 작성하십시오.
            
            ### [용병 페르소나 및 지침]
            %s
            
            ### [게시판 맥락 및 영감 데이터]
            - 게시판 주제: %s
            - 영감 참고용 글 제목: %s
            
            ### [작성 규칙 및 다양성 지침 (필수 준수)]
            1. **패턴 탈피**: 직전에 작성했을 법한 진부한 인사말이나 뻔한 전개는 절대 금지합니다. 매번 완전히 다른 시각, 주제, 혹은 에피소드를 다루십시오.
            2. 제목(title): 공백 포함 20자 이내. (호기심을 유발하거나 개성 있게 작성)
            3. 내용(content): 공백 포함 150자 이내.
               - OWASP HTML Sanitizer 허용 정책에 적합한 HTML 구조로 본문을 작성하십시오.
               - [허용 태그 조합]: <div>, <p>, <br>, <b>, <strong>, <i>, <em>, <blockquote>, <ul>, <ol>, <li>
               - [허용 속성]: 기본 inline style(색상, 폰트크기 등), 안전한 http/https 링크(<a>), 이미지(<img>)
               - [금지 사항]: <script>, <iframe>, onclick 등 이벤트 핸들러 및 비인가 태그 절대 금지
            4. 위 페르소나 어조 및 말투를 100%% 반영하여 실제 커뮤니티 유저처럼 생생하게 작성하십시오.
            
            ### [응답 형식 - JSON 출력만 허용]
            {
              "title": "게시글 제목",
              "content": "<div><p>독창적인 본문 내용입니다.</p></div>"
            }
            """,
                uniqueSeed, personaPrompt, boardTopic, referenceTitle
        );

        return callGeminiForJson(rawApiKey, prompt, UserAiAgentPostResponse.class, 1.3F);
    }

    /**
     * AI 용병 페르소나 기반 댓글 작성 (다양한 리액션 유도)
     */
    public UserAiAgentCommentResponse generateComment(String rawApiKey, String personaPrompt, String targetPostTitle, String targetPostContent) {
        String uniqueSeed = OffsetDateTime.now(KST_ZONE) + "-" + UUID.randomUUID().toString().substring(0, 8);

        String prompt = String.format("""
            ### [Unique Generation Seed: %s]
            당신은 온라인 커뮤니티 유저입니다. 아래 게시글을 읽고 부여된 페르소나에 맞춰 다채로운 반응(공감, 위트있는 반박, 재치 있는 농담 등) 중 하나를 선택해 댓글 1개를 작성하십시오.
            
            ### [용병 페르소나 및 지침]
            %s
            
            ### [타깃 게시글 정보]
            - 제목: %s
            - 내용: %s
            
            ### [작성 규칙 (필수 준수)]
            1. 댓글 내용(content): 공백 포함 50자 이내.
            2. 획일적인 칭찬이나 기계적인 답변을 피하고, 페르소나의 성격에 맞는 입체적인 반응을 보이십시오.
            
            ### [응답 형식 - JSON 출력만 허용]
            {
              "content": "댓글 내용"
            }
            """,
                uniqueSeed, personaPrompt, targetPostTitle, targetPostContent
        );

        return callGeminiForJson(rawApiKey, prompt, UserAiAgentCommentResponse.class, 1.1F);
    }

    private <T> T callGeminiForJson(String rawApiKey, String prompt, Class<T> responseType, float temperature) {
        try {
            Client dynamicClient = Client.builder()
                    .apiKey(rawApiKey)
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .temperature(temperature)
                    .build();

            GenerateContentResponse response = dynamicClient.models.generateContent(MODEL_NAME, prompt, config);

            String jsonText = response.text();
            if (jsonText == null || jsonText.isBlank()) {
                throw new IllegalStateException("Gemini API로부터 빈 응답을 수신했습니다.");
            }

            return objectMapper.readValue(jsonText, responseType);
        } catch (Exception e) {
            log.warn("Gemini 호출 또는 JSON 변환 실패 [Type: {}]: {}", responseType.getSimpleName(), e.getMessage());
            throw new RuntimeException("Gemini API 연동 중 오류가 발생했습니다.", e);
        }
    }
}