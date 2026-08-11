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

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAiAgentGeminiClient {

    private final ObjectMapper objectMapper;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final String MODEL_NAME = "gemini-3.1-flash-lite-preview";

    /**
     * Gemini API Key 유효성 검증
     * 최소한의 요청(토큰)을 보내어 정상 응답 여부로 키를 검증합니다.
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
     * AI 용병 페르소나 기반 게시글 작성
     */
    public UserAiAgentPostResponse generatePost(String rawApiKey, String personaPrompt, String boardTopic, String latestPostTitle) {
        String prompt = String.format("""
            ### [Generation Seed: %s]
            당신은 온라인 커뮤니티 유저이며, 설정된 페르소나에 맞춰 작성 대상 게시판에 게시글 1개를 작성해야 합니다.
            
            ### [용병 페르소나 및 지침]
            %s
            
            ### [게시판 맥락 데이터]
            - 게시판 주제: %s
            - 최근 올라온 게시글 제목: %s
            
            ### [작성 규칙 (필수 준수)]
            1. 제목(title): 공백 포함 20자 이내.
            2. 내용(content): 공백 포함 150자 이내.
               - OWASP HTML Sanitizer 허용 정책에 적합한 HTML 구조로 본문을 작성하십시오.
               - [허용 태그 조합]: <div>, <p>, <br>, <b>, <strong>, <i>, <em>, <blockquote>, <ul>, <ol>, <li>
               - [허용 속성]: 기본 inline style(색상, 폰트크기 등), 안전한 http/https 링크(<a>), 이미지(<img>)
               - [금지 사항]: <script>, <iframe>, onclick 등 이벤트 핸들러 및 비인가 태그 절대 금지
            3. 위 페르소나 어조 및 말투를 100%% 반영하여 자연스러운 커뮤니티 글로 작성하십시오.
            
            ### [응답 형식 - JSON 출력만 허용]
            {
              "title": "게시글 제목",
              "content": "<div><p>게시글 본문 첫 번째 단락입니다.</p><p><b>강조할 내용</b>과 함께 작성되었습니다.</p></div>"
            }
            """,
                OffsetDateTime.now(KST_ZONE), personaPrompt, boardTopic, latestPostTitle
        );

        return callGeminiForJson(rawApiKey, prompt, UserAiAgentPostResponse.class, 1.0F);
    }

    /**
     * AI 용병 페르소나 기반 댓글 작성
     */
    public UserAiAgentCommentResponse generateComment(String rawApiKey, String personaPrompt, String targetPostTitle, String targetPostContent) {
        String prompt = String.format("""
            ### [Generation Seed: %s]
            당신은 온라인 커뮤니티 유저입니다. 아래 게시글을 읽고 부여된 페르소나에 맞춰 댓글 1개를 작성하십시오.
            
            ### [용병 페르소나 및 지침]
            %s
            
            ### [타깃 게시글 정보]
            - 제목: %s
            - 내용: %s
            
            ### [작성 규칙 (필수 준수)]
            1. 댓글 내용(content): 공백 포함 50자 이내.
            2. 부여된 페르소나의 말투, 성향, 반응 방식을 완벽히 재현하십시오.
            
            ### [응답 형식 - JSON 출력만 허용]
            {
              "content": "댓글 내용"
            }
            """,
                OffsetDateTime.now(KST_ZONE), personaPrompt, targetPostTitle, targetPostContent
        );

        return callGeminiForJson(rawApiKey, prompt, UserAiAgentCommentResponse.class, 1.0F);
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