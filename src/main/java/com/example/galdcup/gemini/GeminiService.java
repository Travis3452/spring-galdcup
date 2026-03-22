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

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private Client client;
    private final ObjectMapper objectMapper;
    private final BoardValidator boardValidator;

    @PostConstruct
    public void init() {
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    public GeminiResponse getRecommendation(Long boardId) {

        Board board = boardValidator.findByIdOrThrow(boardId);

        String prompt = String.format(
                "당신은 대한민국 인터넷 커뮤니티 트렌드에 정통한 '갈드컵(논쟁 유발형 투표)' 전문 콘텐츠 플래너입니다.\n\n" +
                        "### [서비스 배경]\n" +
                        "이 서비스는 유저들이 특정 주제에 대해 치열하게 토론하고 투표하는 '갈드컵' 플랫폼입니다.\n" +
                        "게시판 제목: [%s]\n" +
                        "게시판 설명: [%s]\n\n" +
                        "### [미션]\n" +
                        "위 게시판의 주제와 맥락에 완벽히 부합하면서도, 유저들이 댓글로 싸울 만큼 논쟁적인(Controversial) 투표 세션을 기획하세요.\n\n" +
                        "### [작성 가이드라인]\n" +
                        "1. 주제(Topic): 한쪽으로 의견이 쏠리지 않는 '황금 밸런스'를 유지하는 주제여야 합니다.\n" +
                        "2. 선택지(Options): 최소 2개에서 최대 10개까지 생성하되, 각 항목은 서로 독립적이고 매력적이어야 합니다.\n" +
                        "3. 톤앤매너: 딱딱한 말투보다는 유머러스하거나 커뮤니티에서 자주 쓰이는 재치 있는 표현을 선호합니다.\n" +
                        "4. 이미지 고려: 각 선택지는 시각적으로 상상하기 쉽고 명확한 단어(명사 위주)여야 합니다.\n\n" +
                        "### [응답 형식]\n" +
                        "반드시 아래의 JSON 구조로만 응답하세요. 다른 부가 설명은 생략합니다.\n" +
                        "{\n" +
                        "  \"topic\": \"사람들이 환장하는 논쟁적 주제\",\n" +
                        "  \"description\": \"투표 참여를 독려하는 자극적이고 짧은 한 줄 설명\",\n" +
                        "  \"options\": [\"선택지1\", \"선택지2\", ...]\n" +
                        "}",
                board.getTopic(), board.getDescription()
        );

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .temperature(0.7F)
                    .build();

            GenerateContentResponse response = client.models.generateContent("gemini-2.5-flash", prompt, config);

            String jsonResponse = response.text();
            if (jsonResponse == null || jsonResponse.isBlank()) {
                throw new IllegalStateException("API로부터 빈 응답을 받았습니다.");
            }

            return objectMapper.readValue(jsonResponse, GeminiResponse.class);

        } catch (Exception e) {
            log.error("Gemini 추천 생성 실패: {}", e.getMessage(), e);

            return new GeminiResponse(
                    "추천 주제를 불러올 수 없습니다.",
                    "직접 창의적인 투표를 만들어보세요!",
                    List.of("직접 입력 1", "직접 입력 2")
            );
        }
    }
}