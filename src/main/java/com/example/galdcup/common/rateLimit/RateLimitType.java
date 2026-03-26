package com.example.galdcup.common.rateLimit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RateLimitType {
    INTERNAL("internal"), // 내부 API 요청 시
    EXTERNAL("external"); // 외부 API 요청 포함 시

    private final String prefix;
}