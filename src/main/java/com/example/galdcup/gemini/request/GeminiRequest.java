package com.example.galdcup.gemini.request;

import java.util.List;

public record GeminiRequest(
        String topic,
        String description,
        List<String> options
) {}