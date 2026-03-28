package com.example.galdcup.gemini;

import com.example.galdcup.board.domain.Board;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.gemini.response.GeminiResponse;
import com.example.galdcup.gemini.response.OpinionAnalysisResponse;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

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
    public GeminiResponse getRecommendation(Long boardId) {

        Board board = boardValidator.findByIdOrThrow(boardId);

        String prompt = String.format(
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
                board.getTopic(), board.getDescription()
        );

        try {
            // JSON 응답 강제 및 답변 확산도(Temperature) 설정
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .temperature(0.8F)
                    .build();

            // Gemini 3 Flash 모델 호출
            GenerateContentResponse response = client.models.generateContent("gemini-3-flash-preview", prompt, config);

            String jsonResponse = response.text();
            if (jsonResponse == null || jsonResponse.isBlank()) {
                throw new IllegalStateException("Gemini API로부터 빈 응답 수신");
            }

            // 수신한 JSON 텍스트를 DTO 객체로 변환
            return objectMapper.readValue(jsonResponse, GeminiResponse.class);

        } catch (Exception e) {
            log.error("AI 추천 콘텐츠 생성 실패 (게시판 ID: {}): {}", boardId, e.getMessage());

            // API 호출 실패 시 기본 데이터 반환
            return new GeminiResponse(
                    "추천 주제를 불러올 수 없음",
                    "직접 창의적인 투표를 만들어보세요",
                    List.of("직접 입력 1", "직접 입력 2")
            );
        }
    }

    /**
     * 수집된 여론(댓글)을 분석하여 후보별 예상 지지율 산출
     */
    public OpinionAnalysisResponse analyzeOpinion(String topic, String description, List<String> candidates, String comments) {
        String prompt = String.format(
                "당신은 온라인 커뮤니티의 민심을 분석하는 '갈드컵 여론 조사관'입니다.\n\n" +
                        "### [데이터]\n" +
                        "- 주제: %s\n" +
                        "- 설명: %s\n" +
                        "- 후보: %s\n" +
                        "- 수집된 댓글:\n%s\n\n" +
                        "### [미션]\n" +
                        "제공된 댓글을 바탕으로 각 후보의 현재 지지율(%%)을 예측하세요. 합계는 반드시 100%%여야 합니다.\n" +
                        "오직 JSON 데이터만 반환하십시오.\n\n" +
                        "### [응답 형식]\n" +
                        "{\n" +
                        "  \"results\": [\n" +
                        "    { \"label\": \"후보1\", \"supportRate\": 60.0 },\n" +
                        "    { \"label\": \"후보2\", \"supportRate\": 40.0 }\n" +
                        "  ]\n" +
                        "}",
                topic, description, candidates, comments
        );

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .temperature(0.3F)
                    .build();

            GenerateContentResponse response = client.models.generateContent("gemini-3-flash-preview", prompt, config);
            return objectMapper.readValue(response.text(), OpinionAnalysisResponse.class);
        } catch (Exception e) {
            log.error("여론 분석 실패: {}", e.getMessage());
            return OpinionAnalysisResponse.defaultResponse(candidates);
        }
    }
}