package com.example.galdcup.common.client;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Gemini JSON 응답 생성과 변환을 담당하는 공통 클라이언트. */
@Component
@RequiredArgsConstructor
public class GeminiJsonClient {

    private static final String MODEL_NAME = "gemini-3.5-flash-lite";
    private final ObjectMapper objectMapper;

    public <T> T generate(String apiKey, String prompt, Class<T> responseType, float temperature) {
        try (Client client = Client.builder().apiKey(apiKey).build()) {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .temperature(temperature)
                    .build();

            GenerateContentResponse response = client.models.generateContent(MODEL_NAME, prompt, config);
            String json = response.text();
            if (json == null || json.isBlank()) {
                throw new IllegalStateException("Gemini API로부터 빈 응답을 수신했습니다.");
            }
            return objectMapper.readValue(json, responseType);
        }
    }
}
