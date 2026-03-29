package com.example.galdcup.gemini.response;

import java.util.List;

/**
 * AI가 직접 작성한 생성한 댓글 데이터 응답
 */
public record CommentContextResponse(
        List<String> comments
) {}