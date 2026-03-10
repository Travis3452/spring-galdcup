package com.example.galdcup.postCategory;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.postCategory.dto.PostCategoryDto;
import com.example.galdcup.postCategory.dto.PostCategoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/post-categories")
@RequiredArgsConstructor
public class PostCategoryController {

    private final PostCategoryService postCategoryService;

    @GetMapping
    public ResponseEntity<List<PostCategoryDto>> getBoardPostCategories(@PathVariable Long boardId) {
        return ResponseEntity.ok(postCategoryService.findByBoardId(boardId));
    }

    @PostMapping
    public ResponseEntity<PostCategoryDto> createPostCategory(@PathVariable Long boardId,
                                                              @RequestBody PostCategoryRequest request,
                                                              @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postCategoryService.createCustomPostCategory(request, boardId, principal.getId()));
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<PostCategoryDto> updatePostCategory(@PathVariable Long boardId,
                                                              @PathVariable Long categoryId,
                                                              @RequestBody PostCategoryRequest request,
                                                              @AuthenticationPrincipal CustomUserDetails principal) {

        return ResponseEntity.ok(postCategoryService.updateCustomPostCategory(request, boardId, categoryId, principal.getId()));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteAndMigratePostCategory(@PathVariable Long boardId,
                                                             @PathVariable Long categoryId,
                                                             @RequestParam Long moveToId,
                                                             @AuthenticationPrincipal CustomUserDetails principal) {

        postCategoryService.deleteAndMigrate(boardId, categoryId, moveToId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}