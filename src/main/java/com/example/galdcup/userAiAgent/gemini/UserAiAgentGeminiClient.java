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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAiAgentGeminiClient {

    private final ObjectMapper objectMapper;
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

        String prompt = String.format("""
            ### [Unique Generation Seed: %s]
            당신은 온라인 커뮤니티 유저입니다. 아래 제공된 [게시판 대세 주제], [게시판 실시간 흐름], 그리고 [자신이 쓴 과거 글 목록]을 종합적으로 분석하여 완전히 독창적인 게시글 1개를 작성하십시오.
            
            ### [용병 페르소나 및 지침]
            %s
            
            ### [게시판 맥락 데이터]
            - 게시판 주제: %s
            
            [게시판 대세 및 핫이슈 (인기글 5개)]
            %s
            
            [게시판 실시간 흐름 (최근글 5개)]
            %s
            
            ### [자신이 작성했던 과거 글 이력 (Negative Context - 절대 중복 금지)]
            %s
            
            ### [작성 규칙 및 전략 (필수 준수)]
            1. **상황별 작성 전략**:
               - **메타 반영**: [인기글 5개]를 참고하여 유저들이 현재 어떤 화두나 영양가 있는 소재에 반응하는지 분위기를 파악하십시오.
               - **실시간 반응**: [최근글 5개]에 올라온 최신 이슈를 꼬리 물기 하거나, 반박/어그로를 끌어 실시간 참여를 유도하십시오.
               - **중복 완전 차단**: [자신이 작성했던 과거 글 이력]에 나온 주제, 메뉴, 논리, 첫 문장 구조는 '절대' 다시 사용하지 마십시오.
            2. 제목(title): 공백 포함 20자 이내. (호기심 유발 및 커뮤니티 톤)
            3. 내용(content): 공백 포함 150자 이내.
               - OWASP HTML Sanitizer 허용 정책에 적합한 HTML 구조로 본문을 작성하십시오(단, 태그와 속성의 사용은 최소화할 것).
               - [허용 태그]: <div>, <p>, <br>, <b>, <strong>, <i>, <em>, <blockquote>, <ul>, <ol>, <li>
               - [허용 속성]: 기본 inline style(색상, 폰트크기 등), 안전한 http/https 링크(<a>), 이미지(<img>)
               - [금지 사항]: <script>, <iframe>, onclick 등 이벤트 핸들러 및 비인가 태그 절대 금지
            4. 위 페르소나 어조 및 말투를 100%% 반영하여 실제 커뮤니티 유저처럼 생생하게 작성하십시오.
            
            ### [응답 형식 - JSON 출력만 허용]
            {
              "title": "게시글 제목",
              "content": "<div><p>독창적인 본문 내용입니다.</p></div>"
            }
            """,
                uniqueSeed, personaPrompt, boardTopic, popularText, latestText, myPastText
        );

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

        String prompt = String.format("""
            ### [Unique Generation Seed: %s]
            당신은 온라인 커뮤니티 유저입니다. 아래 타깃 게시글과 기존 댓글 흐름, 그리고 자신이 과거에 쓴 댓글 이력을 바탕으로 부여된 페르소나에 맞춰 다채롭고 입체적인 댓글 1개를 작성하십시오.
            
            ### [용병 페르소나 및 지침]
            %s
            
            ### [타깃 게시글 정보]
            - 제목: %s
            - 내용: %s
            
            ### [게시글 기존 댓글 흐름 (참고용)]
            %s
            
            ### [자신이 작성했던 과거 댓글 이력 (Negative Context - 중복 금지)]
            %s
            
            ### [작성 규칙 (필수 준수)]
            1. **중복 및 단조로움 탈피**:
               - [게시글 기존 댓글 흐름]과 동일한 멘트나 뻔한 칭찬, 기계적인 감상평을 피하고 시각을 달리하십시오.
               - [자신이 작성했던 과거 댓글 이력]에 자주 쓰인 유행어, 템플릿, 말버릇을 맹목적으로 반복하지 마십시오.
            2. 댓글 내용(content): 공백 포함 50자 이내.
            3. 공감, 위트 있는 반박, 재치 있는 드립, 질문 던지기 등 페르소나의 성격에 부합하는 리액션을 유연하게 선택하십시오.
            
            ### [응답 형식 - JSON 출력만 허용]
            {
              "content": "댓글 내용"
            }
            """,
                uniqueSeed, personaPrompt, targetPostTitle, targetPostContent, postCommentsText, myPastCommentsText
        );

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