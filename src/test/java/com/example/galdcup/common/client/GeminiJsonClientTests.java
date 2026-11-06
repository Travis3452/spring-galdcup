package com.example.galdcup.common.client;

import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GeminiJsonClientTests {
    record Answer(String content) {}

    @Test
    void parsesJsonAndUsesEachRequestsKeyAndTemperature() {
        Client client = mock(Client.class);
        Models models = mock(Models.class);
        ReflectionTestUtils.setField(client, "models", models);
        Client.Builder builder = mock(Client.Builder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(client);
        var response = mock(GenerateContentResponse.class);
        when(response.text()).thenReturn("{\"content\":\"생성 결과\"}");
        when(models.generateContent(anyString(), anyString(), any(GenerateContentConfig.class))).thenReturn(response);

        try (var factory = mockStatic(Client.class)) {
            factory.when(Client::builder).thenReturn(builder);
            var subject = new GeminiJsonClient(new ObjectMapper());
            assertEquals("생성 결과", subject.generate("server-key", "추천 입력", Answer.class, 0.8F).content());
            assertEquals("생성 결과", subject.generate("user-key", "댓글 입력", Answer.class, 1.1F).content());
            verify(builder).apiKey("server-key");
            verify(builder).apiKey("user-key");
            var config = org.mockito.ArgumentCaptor.forClass(GenerateContentConfig.class);
            verify(models).generateContent(eq("gemini-3.5-flash-lite"), eq("추천 입력"), config.capture());
            assertEquals("application/json", config.getValue().responseMimeType().orElseThrow());
            assertEquals(0.8F, config.getValue().temperature().orElseThrow());
            verify(client, times(2)).close();
        }
    }

    @Test
    void rejectsBlankAndMalformedJsonAndClosesClient() {
        Client client = mock(Client.class);
        Models models = mock(Models.class);
        ReflectionTestUtils.setField(client, "models", models);
        Client.Builder builder = mock(Client.Builder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(client);
        var response = mock(GenerateContentResponse.class);
        when(response.text()).thenReturn(" ", "not json");
        when(models.generateContent(anyString(), anyString(), any(GenerateContentConfig.class))).thenReturn(response);

        try (var factory = mockStatic(Client.class)) {
            factory.when(Client::builder).thenReturn(builder);
            var subject = new GeminiJsonClient(new ObjectMapper());
            assertThrows(IllegalStateException.class, () -> subject.generate("key", "input", Answer.class, 0.3F));
            assertThrows(RuntimeException.class, () -> subject.generate("key", "input", Answer.class, 0.3F));
            verify(client, times(2)).close();
        }
    }

    @Test
    void sdkFailurePropagatesAndClosesClient() {
        Client client = mock(Client.class);
        Models models = mock(Models.class);
        ReflectionTestUtils.setField(client, "models", models);
        Client.Builder builder = mock(Client.Builder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(client);
        var failure = new IllegalStateException("SDK failure");
        when(models.generateContent(anyString(), anyString(), any(GenerateContentConfig.class))).thenThrow(failure);

        try (var factory = mockStatic(Client.class)) {
            factory.when(Client::builder).thenReturn(builder);
            var subject = new GeminiJsonClient(new ObjectMapper());
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> subject.generate("key", "input", Answer.class, 0.3F)));
            verify(client).close();
        }
    }
}
