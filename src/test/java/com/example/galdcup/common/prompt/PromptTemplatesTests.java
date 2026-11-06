package com.example.galdcup.common.prompt;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PromptTemplatesTests {
    private final PromptTemplates templates = new PromptTemplates();

    @Test void allFourTemplatesRenderWithTheirNamedInputs() {
        var recommendation = templates.render("vote-recommendation", Map.of("seed", "seed", "boardTopic", "게임", "boardDescription", "설명"));
        assertTrue(recommendation.contains("[게임](설명)"));
        var analysis = templates.render("opinion-analysis", Map.of("seed", "seed", "topic", "주제", "description", "설명", "candidates", "후보", "comments", "댓글"));
        assertTrue(analysis.contains("합계 100% 필수"));
        assertFalse(analysis.contains("100%%"));
        assertTrue(templates.render("agent-post", Map.of("seed", "seed", "persona", "인물", "boardTopic", "게임", "popularPosts", "인기", "latestPosts", "최신", "myPastPosts", "과거")).contains("게임"));
        assertTrue(templates.render("agent-comment", Map.of("seed", "seed", "persona", "인물", "postTitle", "제목", "postContent", "내용", "recentComments", "최근", "myPastComments", "과거")).contains("제목: 제목"));
    }

    @Test void insertedTextIsLiteralAndNeverRenderedAgain() {
        String value = "$1 \\ "+"$"+"{boardDescription}";
        String result = templates.render("vote-recommendation", Map.of("seed", "seed", "boardTopic", value, "boardDescription", "설명"));
        assertTrue(result.contains("[" + value + "](설명)"));
    }

    @Test void missingInputsAndUnknownTemplatesFailClearly() {
        assertThrows(IllegalArgumentException.class, () -> templates.render("agent-comment", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> templates.render("missing", Map.of()));
    }
}
