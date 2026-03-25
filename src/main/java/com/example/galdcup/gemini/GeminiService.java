package com.example.galdcup.gemini;

import com.example.galdcup.board.domain.Board;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.gemini.response.GeminiResponse;
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

        // 2030 남초 커뮤니티 정서 및 '근본론'을 반영한 프롬프트 구성
        String prompt = String.format(
                "당신은 대한민국 2030 남성들이 이용하는 주요 온라인 커뮤니티 정서와 '근본' 문화를 꿰뚫어 보는 갈드컵 설계자입니다.\n\n" +
                        "### [서비스 배경]\n" +
                        "유저들이 특정 주제에 대해 치열하게 토론하고 투표하는 '갈드컵' 플랫폼입니다.\n" +
                        "게시판 주제: [%s]\n" +
                        "게시판 설명: [%s]\n\n" +
                        "### [미션: 논쟁적 투표 기획]\n" +
                        "위 주제를 바탕으로 유저들이 '이건 못 참지'라며 달려들어 밤새 키보드 배틀을 벌일 만한 논쟁적인 투표 세션을 기획하세요.\n\n" +
                        "### [설계 가이드라인]\n" +
                        "1. 투표 성격: 다음 두 가지 중 주제에 더 적합한 방식을 선택하십시오.\n" +
                        "   - [대립형]: 서로 상충하는 두 세력이나 가치관의 치명적인 이지선다 (예: 부먹 vs 찍먹)\n" +
                        "   - [최강자 선발형]: 특정 집단 내에서 '진짜 주인공'이나 'GOAT'를 가리는 내부 서열 정리 (예: BTS 중 최고의 멤버는?, 롤드컵 역대 우승팀 중 최강은?)\n" +
                        "2. 황금 밸런스: 어느 한쪽으로 여론이 쏠리지 않도록 선택지 간의 무게추를 완벽히 맞춰야 합니다.\n" +
                        "3. 근본론: 어떤 선택지가 더 '근본' 있고 정통성이 있는지 유저들이 자존심을 걸고 치열하게 논쟁하도록 유도하세요.\n" +
                        "4. 고유 대상 원칙 (CRITICAL): 각 선택지는 반드시 서로 다른 대상(Entity)을 다뤄야 합니다. 동일한 대상을 수식어만 바꿔서 여러 칸에 배치하는 행위는 엄격히 금지합니다.\n" +
                        "5. 선택지 개수 (최하위 우선순위): 주제의 논리적 완결성에 따라 최소 2개에서 최대 10개 범위 내에서 생성하십시오. 억지로 개수를 늘리기보다 각 선택지의 퀄리티를 최우선으로 하십시오.\n" +
                        "6. 톤앤매너: 커뮤니티 유저가 쓴 것처럼 재치 있고 과몰입을 유도하는 '매운맛' 표현을 사용하세요.\n\n" +
                        "### [응답 형식 - JSON 출력만 허용]\n" +
                        "{\n" +
                        "  \"topic\": \"유저들 혈압 오르게 만드는 황밸 주제\",\n" +
                        "  \"description\": \"댓글 창 화력 폭발시키는 자극적이고 짧은 한 줄 설명\",\n" +
                        "  \"options\": [\"고유 대상1\", \"고유 대상2\", ...]\n" +
                        "}",
                board.getTopic(), board.getDescription()
        );

        try {
            // JSON 응답 강제 및 답변 확산도(Temperature) 설정
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .temperature(0.7F)
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
}