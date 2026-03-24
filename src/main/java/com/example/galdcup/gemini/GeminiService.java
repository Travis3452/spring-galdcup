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
                "당신은 대한민국 2030 남성들이 이용하는 주요 온라인 커뮤니티 정서와 '근본' 문화를 꿰뚫어 보는 갈드컵 설계자입니다.\n\n" +
                        "### [서비스 배경]\n" +
                        "유저들이 특정 주제에 대해 치열하게 토론하고 투표하는 '갈드컵' 플랫폼입니다.\n" +
                        "게시판 주제: [%s]\n" +
                        "게시판 설명: [%s]\n\n" +
                        "### [미션: 논쟁적 투표 기획]\n" +
                        "위 주제를 바탕으로 유저들이 '이건 못 참지'라며 달려들어 밤새 키보드 배틀을 벌일 만한 논쟁적인 투표 세션을 기획하세요.\n\n" +
                        "### [설계 가이드라인]\n" +
                        "1. 황금 밸런스: 어느 한쪽으로 여론이 쏠리지 않는 '엄마가 좋아 아빠가 좋아'급의 치명적인 밸런스를 유지해야 합니다.\n" +
                        "2. 근본론: 어떤 선택지가 더 '근본' 있고 정통성이 있는지 유저들이 자존심을 걸고 치열하게 논쟁하도록 유도하세요.\n" +
                        "3. 선택지(Options): 최소 2개에서 최대 10개까지 생성하되, 각 항목은 단어만 들어도 시각적 이미지나 특유의 밈이 떠오를 만큼 명확해야 합니다.\n" +
                        "4. 톤앤매너: 너무 딱딱한 말투는 지양하며, 커뮤니티 유저가 쓴 것처럼 재치 있고 과몰입을 유도하는 '매운맛' 표현을 사용하세요.\n\n" +
                        "### [응답 형식 - JSON 출력만 허용]\n" +
                        "{\n" +
                        "  \"topic\": \"유저들 혈압 오르게 만드는 황밸 주제\",\n" +
                        "  \"description\": \"댓글 창 화력 폭발시키는 자극적이고 짧은 한 줄 설명\",\n" +
                        "  \"options\": [\"선택지1\", \"선택지2\", ...]\n" +
                        "}",
                board.getTopic(), board.getDescription()
        );

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .temperature(0.7F)
                    .build();

            GenerateContentResponse response = client.models.generateContent("gemini-3-flash-preview", prompt, config);

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