package com.example.galdcup.post;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.post.domain.PostReaction;
import com.example.galdcup.post.request.CreatePostRequest;
import com.example.galdcup.post.request.UpdatePostRequest;
import com.example.galdcup.post.response.PostDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Post", description = "게시글 작성, 조회 및 반응(좋아요/싫어요) API")
public interface PostApi {

    @Operation(summary = "게시판별 게시글 목록 조회", description = "특정 게시판의 게시글을 페이징 조회합니다.")
    ResponseEntity<Page<PostDto>> getPosts(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "카테고리 ID (선택)") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "인기 게시글 여부") @RequestParam(defaultValue = "false") boolean isPopular,
            @Parameter(description = "검색 타입 (TITLE, CONTENT, AUTHOR 등)") @RequestParam(required = false) String searchType,
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            Pageable pageable);

    @Operation(summary = "게시글 단건 조회", description = "특정 게시글의 상세 내용을 조회합니다.")
    ResponseEntity<PostDto> getPost(@Parameter(description = "게시글 ID") @PathVariable Long id);

    @Operation(summary = "사용자별 작성 게시글 목록 조회", description = "특정 닉네임의 사용자가 작성한 모든 게시글을 조회합니다.")
    ResponseEntity<Page<PostDto>> getPostsByUser(
            @Parameter(description = "사용자 닉네임") @PathVariable String nickname,
            Pageable pageable);

    @Operation(summary = "게시글 작성", description = "사용자가 새로운 게시글을 작성합니다.")
    ResponseEntity<PostDto> createPost(
            @RequestBody CreatePostRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "게시글 수정", description = "본인이 작성한 게시글을 수정합니다.")
    ResponseEntity<PostDto> updatePost(
            @Parameter(description = "게시글 ID") @PathVariable Long id,
            @RequestBody UpdatePostRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "게시글 삭제 (작성자)", description = "본인이 작성한 게시글을 삭제합니다.")
    ResponseEntity<Void> deletePost(
            @Parameter(description = "게시글 ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "게시글 삭제 (관리자)", description = "게시판 관리자가 게시글을 강제 삭제합니다.")
    ResponseEntity<Void> deletePostByManager(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "게시글 좋아요/싫어요", description = "게시글에 대한 좋아요/싫어요를 추가합니다.")
    ResponseEntity<Void> addReaction(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(description = "반응 타입 (LIKE, DISLIKE)") @RequestParam PostReaction.ReactionType type,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);
}