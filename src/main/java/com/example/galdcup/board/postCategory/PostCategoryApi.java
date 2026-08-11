package com.example.galdcup.board.postCategory;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.board.postCategory.request.PostCategoryRequest;
import com.example.galdcup.board.postCategory.request.UpdatePostCategoryRequest;
import com.example.galdcup.board.postCategory.response.PostCategoryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Post Category", description = "게시판 카테고리 설정 및 관리 API")
public interface PostCategoryApi {

    @Operation(summary = "게시판 카테고리 목록 조회", description = "특정 게시판에 속한 모든 카테고리를 조회합니다.")
    ResponseEntity<List<PostCategoryDto>> getBoardPostCategories(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId);

    @Operation(summary = "카테고리 생성", description = "관리자 권한으로 게시판 내에 새로운 카테고리를 생성합니다.")
    ResponseEntity<PostCategoryDto> createPostCategory(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @RequestBody PostCategoryRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "단일 카테고리 수정", description = "특정 카테고리의 이름이나 정렬 순서를 개별적으로 수정합니다.")
    ResponseEntity<PostCategoryDto> updatePostCategory(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @RequestBody UpdatePostCategoryRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "카테고리 일괄 수정", description = "게시판 내 여러 카테고리의 순서를 한꺼번에 변경합니다.")
    ResponseEntity<Void> updateCategoriesBatch(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @RequestBody List<UpdatePostCategoryRequest> requests,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "카테고리 삭제 및 이관", description = "카테고리를 삭제하고, 해당 카테고리에 속했던 게시글들을 다른 카테고리로 안전하게 이동시킵니다.")
    ResponseEntity<Void> deleteAndMigratePostCategory(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "삭제할 카테고리 ID") @PathVariable Long categoryId,
            @Parameter(description = "게시글을 옮길 카테고리 ID") @RequestParam Long moveToId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);
}