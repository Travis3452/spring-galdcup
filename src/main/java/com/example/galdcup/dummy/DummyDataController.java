package com.example.galdcup.dummy;

import com.example.galdcup.common.rateLimit.RateLimit;
import com.example.galdcup.common.rateLimit.RateLimitType;
import com.example.galdcup.common.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/dummy")
@RequiredArgsConstructor
@Slf4j
public class DummyDataController implements DummyDataApi {

    private final DummyDataService dummyDataService;

    /**
     * 게시글 10개 생성 (AI 기획 데이터 기반)
     */
    @RateLimit(type = RateLimitType.EXTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/board/{boardId}/posts")
    public ResponseEntity<String> createDummyPosts(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        try {
            dummyDataService.generateDummyPosts(boardId, principal.getId());
            return ResponseEntity.ok("게시판에 AI 기획 더미 게시글 10개가 생성되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 댓글 20개 생성 (AI 기획 데이터 기반)
     */
    @RateLimit(type = RateLimitType.EXTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/board/{boardId}/comments")
    public ResponseEntity<String> createDummyComments(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        try {
            dummyDataService.generateDummyComments(boardId, principal.getId());
            return ResponseEntity.ok("게시판에 20개의 더미 댓글이 생성되었습니다.");
        } catch (Exception e) {
            log.error("댓글 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body("❌ 생성 실패: " + e.getMessage());
        }
    }
}