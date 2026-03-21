package com.example.galdcup.post;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.post.domain.PostReaction;
import com.example.galdcup.post.request.CreatePostRequest;
import com.example.galdcup.post.response.PostDto;
import com.example.galdcup.post.request.UpdatePostRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시판별 게시글 목록 조회
     */
    @GetMapping("/board/{boardId}")
    public ResponseEntity<Page<PostDto>> getPosts(
            @PathVariable Long boardId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "false") boolean isPopular,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostDto> posts = postService.getPosts(
                boardId, categoryId, isPopular, searchType, keyword, pageable);

        return ResponseEntity.ok(posts);
    }

    /**
     * 게시글 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostDto> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.findById(id));
    }

    /**
     * 사용자별 작성 게시글 목록 조회
     */
    @GetMapping("/user/{nickname}")
    public ResponseEntity<Page<PostDto>> getPostsByUser(
            @PathVariable String nickname,
            Pageable pageable) {
        Page<PostDto> posts = postService.findByAuthorNickname(nickname, pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시글 작성
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<PostDto> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        PostDto saved = postService.create(request.boardId(), request.categoryId(), principal.getId(), request.title(), request.content());
        return ResponseEntity.ok(saved);
    }

    /**
     * 게시글 수정
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<PostDto> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        PostDto updated = postService.update(id, request.categoryId(), principal.getId(), request.title(), request.content());

        return ResponseEntity.ok(updated);
    }

    /**
     * 게시글 삭제 (작성자 본인)
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        postService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글 삭제 (게시판 관리자 전용)
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/board/{boardId}/post/{postId}")
    public ResponseEntity<Void> deletePostByManager(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        postService.deleteForBoardManager(postId, boardId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글 좋아요/싫어요 추가
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{postId}/reactions")
    public ResponseEntity<Void> addReaction(
            @PathVariable Long postId,
            @RequestParam PostReaction.ReactionType type,
            @AuthenticationPrincipal CustomUserDetails principal) {

        postService.addReaction(postId, principal.getId(), type);
        return ResponseEntity.ok().build();
    }
}