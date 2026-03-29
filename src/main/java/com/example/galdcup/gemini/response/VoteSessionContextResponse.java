package com.example.galdcup.gemini.response;

import java.util.List;

public record VoteSessionContextResponse(
        String topic,
        String description,
        List<String> options
) {}