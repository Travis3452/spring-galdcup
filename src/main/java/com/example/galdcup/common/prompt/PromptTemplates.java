package com.example.galdcup.common.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 시작 시 프롬프트를 읽고, 원본 템플릿의 자리표시자만 한 번 치환합니다. */
@Component
public class PromptTemplates {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9]*)\\}");
    private final Map<String, String> templates;

    public PromptTemplates() {
        Map<String, String> loaded = new HashMap<>();
        for (String name : List.of("vote-recommendation", "opinion-analysis", "agent-post", "agent-comment")) {
            var resource = new ClassPathResource("prompts/" + name + ".txt");
            try (var input = resource.getInputStream()) {
                loaded.put(name, new String(input.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException ex) {
                throw new IllegalStateException("프롬프트 파일을 읽을 수 없습니다: " + name, ex);
            }
        }
        templates = Map.copyOf(loaded);
    }

    public String render(String name, Map<String, ?> values) {
        String template = templates.get(name);
        if (template == null) throw new IllegalArgumentException("알 수 없는 프롬프트: " + name);
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!values.containsKey(key)) {
                throw new IllegalArgumentException("프롬프트 입력값이 없습니다: " + name + "/" + key);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(Objects.toString(values.get(key))));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
