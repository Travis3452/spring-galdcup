package com.example.galdcup.gemini.response;

import java.util.List;

/**
 * AI가 생성한 게시글 데이터 응답
 */
public record PostContextResponse(
        List<PostData> posts
) {
    /**
     * 개별 게시글의 제목과 본문 쌍
     */
    public record PostData(
            String title,
            String content
    ) {}
}