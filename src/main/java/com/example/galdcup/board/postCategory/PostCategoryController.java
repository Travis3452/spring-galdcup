package com.example.galdcup.board.postCategory;

import com.example.galdcup.common.rateLimit.RateLimit;
import com.example.galdcup.common.rateLimit.RateLimitType;
import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.board.postCategory.request.PostCategoryRequest;
import com.example.galdcup.board.postCategory.request.UpdatePostCategoryRequest;
import com.example.galdcup.board.postCategory.response.PostCategoryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/post-categories")
@RequiredArgsConstructor
public class PostCategoryController implements PostCategoryApi {

    private final PostCategoryService postCategoryService;

    /**
     * 게시판 카테고리 목록 조회 (정렬 순서 포함)
     */
    @GetMapping
    public ResponseEntity<List<PostCategoryDto>> getBoardPostCategories(@PathVariable Long boardId) {
        return ResponseEntity.ok(postCategoryService.findByBoardId(boardId));
    }

    /**
     * 카테고리 생성
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<PostCategoryDto> createPostCategory(@PathVariable Long boardId,
                                                              @RequestBody @Valid PostCategoryRequest request,
                                                              @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postCategoryService.createCustomPostCategory(request, boardId, principal.getId()));
    }

    /**
     * 단일 카테고리 수정 (이름 및 정렬 순서)
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PatchMapping
    public ResponseEntity<PostCategoryDto> updatePostCategory(@PathVariable Long boardId,
                                                              @RequestBody @Valid UpdatePostCategoryRequest request,
                                                              @AuthenticationPrincipal CustomUserDetails principal) {

        return ResponseEntity.ok(postCategoryService.updateCategory(boardId, request, principal.getId()));
    }

    /**
     * 카테고리 일괄 수정 (전체 순서 재정렬)
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/batch")
    public ResponseEntity<Void> updateCategoriesBatch(@PathVariable Long boardId,
                                                      @RequestBody @Valid List<UpdatePostCategoryRequest> requests,
                                                      @AuthenticationPrincipal CustomUserDetails principal) {

        postCategoryService.updateCategoryBatch(boardId, requests, principal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 카테고리 삭제 및 게시글 이관
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteAndMigratePostCategory(@PathVariable Long boardId,
                                                             @PathVariable Long categoryId,
                                                             @RequestParam Long moveToId,
                                                             @AuthenticationPrincipal CustomUserDetails principal) {

        postCategoryService.deleteAndMigrate(boardId, categoryId, moveToId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}