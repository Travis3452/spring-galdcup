package com.example.galdcup.aiagent;

import com.example.galdcup.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Test Utils", description = "개발 및 테스트용 더미 데이터 생성 API")
public interface AiAgentApi {

    @Operation(summary = "더미 게시글 생성", description = "Gemini API를 기반으로 특정 게시판에 10개의 게시글을 생성합니다.")
    ResponseEntity<String> createDummyPosts(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "더미 댓글 생성", description = "Gemini API를 기반으로 특정 게시판 내 게시글들에 총 20개의 댓글을 생성합니다.")
    ResponseEntity<String> createDummyComments(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);
}