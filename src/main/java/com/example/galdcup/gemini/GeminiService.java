package com.example.galdcup.gemini;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.gemini.response.CommentContextResponse;
import com.example.galdcup.gemini.response.OpinionAnalysisResponse;
import com.example.galdcup.gemini.response.PostContextResponse;
import com.example.galdcup.gemini.response.VoteSessionContextResponse;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 생성형 AI(Gemini)를 활용한 투표 세션 자동 생성 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private Client client;
    private final ObjectMapper objectMapper;
    private final BoardValidator boardValidator;

    /** 구글 Generative AI 클라이언트 */
    @PostConstruct
    public void init() {
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * 게시판 주제와 설명을 바탕으로 AI가 기획한 갈드컵 투표 세션 추천
     */
    public VoteSessionContextResponse getRecommendation(Long boardId) {
        Board board = boardValidator.findByIdOrThrow(boardId);

        String prompt = String.format(
                "### [System Seed: %s]\n" +
                        "당신은 대한민국 2030 남성들이 이용하는 주요 온라인 커뮤니티 정서와 '근본' 문화를 꿰뚫어 보는 갈드컵 설계자입니다.\n\n" +
                        "### [데이터 제약 사항 (필수 준수)]\n" +
                        "1. 주제(topic): 공백 포함 **50자 이내** (자극적이고 핵심적인 제목)\n" +
                        "2. 전체 설명(description): 공백 포함 **255자 이내** (모든 선택지에 대한 조건이나 페널티, 부연 설명을 여기에 몰아서 작성하십시오)\n" +
                        "3. 선택지(options): 각 항목당 **20자 이내** (문자열 배열 형식)\n" +
                        "4. 선택지 개수: 최소 2개에서 최대 10개 사이\n\n" +
                        "### [미션: 논쟁적 투표 기획]\n" +
                        "게시판 주제 [%s](%s)에 맞춰 유저들이 밤새 토론할 만한 투표를 기획하세요.\n\n" +
                        "### [설계 가이드라인 - 3가지 유형]\n" +
                        "다음 중 가장 적합한 방식을 선택하여 기획하십시오:\n" +
                        "- [대립형]: 서로 상충하는 두 세력이나 가치관의 이지선다 (예: 부먹 vs 찍먹)\n" +
                        "- [최강자 선발형]: 특정 집단 내에서 'GOAT'를 가리는 서열 정리 (예: 역대 롤드컵 우승팀 중 최강은?)\n" +
                        "- [밸런스 게임형]: 논리적 이득과 감성적 가치가 충돌하는 선택 (예: 평생 돼지고기만 먹기 vs 평생 소고기만 먹기)\n\n" +
                        "### [출력 규칙 (CRITICAL)]\n" +
                        "- **선택지(options)는 단순 문자열 배열 형식을 사용하십시오.** (예: [\"대상1\", \"대상2\"])\n" +
                        "- **선택지에 '단, ~' 와 같은 제약 조건은 절대 넣지 마십시오. 조건은 오직 description에만 작성합니다.**\n" +
                        "- **다만, 대상의 전성기나 특징을 강조하기 위한 '간단한 수식어'는 허용합니다.** (예: '전성기 타이슨', '2015 T1', '군대 가기 전날 먹는 치킨')\n" +
                        "- 톤앤매너: 커뮤니티 유저가 쓴 것처럼 재치 있고 과몰입을 유도하는 '매운맛' 표현을 사용하세요.\n\n" +
                        "### [응답 형식 - JSON 출력만 허용]\n" +
                        "{\n" +
                        "  \"topic\": \"50자 이내 주제\",\n" +
                        "  \"description\": \"255자 이내의 상세 조건 및 설명\",\n" +
                        "  \"options\": [\"투표 선택지 명칭1\", \"투표 선택지 명칭2\"]\n" +
                        "}",
                LocalDateTime.now(), board.getTopic(), board.getDescription()
        );

        return callGeminiForJson(prompt, VoteSessionContextResponse.class, 0.8F);
    }

    /**
     * 수집된 여론(댓글)을 분석하여 후보별 예상 지지율 및 AI 분석평 산출
     */
    public OpinionAnalysisResponse analyzeOpinion(String topic, String description, List<String> candidates, String comments) {
        String prompt = String.format("""
            ### [Analysis Seed: %s]
            당신은 대한민국 온라인 커뮤니티의 민심을 꿰뚫어 보는 '갈드컵 전문 분석관'입니다.
            제공된 댓글 데이터를 바탕으로 현재의 여론 지형을 분석하세요.
    
            ### [데이터]
            - 주제: %s
            - 설명: %s
            - 후보: %s
            - 수집된 댓글 소스:
            %s
    
            ### [미션]
            1. 각 후보의 지지율(%%)을 예측하세요. (합계 100%% 필수)
            2. 현재 커뮤니티 여론의 핵심 흐름과 논쟁 포인트를 짚어주는 **분석 요약(summary)**을 작성하세요.
            3. [매우 중요] 응답 JSON의 `label` 값은 위에 제공된 [후보] 목록의 텍스트를 **토씨 하나 틀리지 않고 정확하게 그대로** 사용해야 합니다. (임의 수정, 영문 번역, 기호 추가, 축약 절대 금지)
            4. [매우 중요] 결과(`results`) 배열의 항목 수는 제공된 **[후보]의 전체 개수와 정확히 일치**해야 합니다. 제공된 후보를 누락하거나, 없는 후보를 임의로 추가하지 마십시오.
    
            ### [요약 작성 가이드라인]
            - 톤앤매너: 커뮤니티 유저들이 공감할 수 있는 날카롭고 재치 있는 문체 (예: "~하는 분위기가 지배적임", "~가 치명타로 작용한 듯함")
            - 분량: 공백 포함 100~150자 내외로 강렬하게 작성하십시오.
    
            ### [응답 형식 - JSON 출력만 허용]
            (주의: 아래 results 배열의 항목 수는 단순 예시입니다. 반드시 실제 제공된 후보의 개수(2~10개)만큼 객체를 생성하십시오.)
            {
              "results": [
                { "label": "제공된 첫 번째 후보 이름 그대로", "supportRate": 60.0 },
                { "label": "제공된 두 번째 후보 이름 그대로", "supportRate": 40.0 }
              ],
              "summary": "현재 여론은..."
            }
            """,
                LocalDateTime.now(), topic, description, String.join(", ", candidates), comments
        );

        return callGeminiForJson(prompt, OpinionAnalysisResponse.class, 0.3F);
    }

    /**
     * Gemini API 호출 및 JSON 파싱 처리
     */
    private <T> T callGeminiForJson(String prompt, Class<T> responseType, float temperature) {
        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .temperature(temperature)
                    .build();

            GenerateContentResponse response = client.models.generateContent("gemini-3.5-flash-lite", prompt, config);

            String jsonResponse = response.text();
            if (jsonResponse == null || jsonResponse.isBlank()) {
                throw new IllegalStateException("Gemini API로부터 빈 응답 수신");
            }

            return objectMapper.readValue(jsonResponse, responseType);
        } catch (Exception e) {
            log.warn("Gemini 호출 또는 JSON 변환 오류 [Type: {}]: {}", responseType.getSimpleName(), e.getMessage());
            throw new RuntimeException("AI 서비스 통신 오류", e);
        }
    }
}