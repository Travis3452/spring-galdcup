package com.example.galdcup.post;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.post.dto.CreatePostRequest;
import com.example.galdcup.post.dto.PostDto;
import com.example.galdcup.post.dto.UpdatePostRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시판별 게시글 목록 조회
     */
    @GetMapping("/board/{boardId}")
    public ResponseEntity<Page<PostDto>> getPostsByBoard(@PathVariable Long boardId,
                                                         Pageable pageable) {
        Page<PostDto> posts = postService.findByBoard(boardId, pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * 사용자별 게시글 목록 조회
     */
    @GetMapping("/user/{nickname}")
    public ResponseEntity<Page<PostDto>> getPostsByUser(@PathVariable String nickname,
                                                        Pageable pageable) {
        Page<PostDto> posts = postService.findByAuthorNickname(nickname, pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시글 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostDto> getPost(@PathVariable Long id) {
        Optional<PostDto> postOpt = postService.findById(id);
        return postOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 게시판의 게시글 검색(제목+내용)
     */
    @GetMapping("/board/{boardId}/search/keyword")
    public ResponseEntity<Page<PostDto>> searchPostsByTitleAndContent(@PathVariable Long boardId,
                                                                      @RequestParam String keyword,
                                                                     Pageable pageable) {
        Page<PostDto> posts = postService.getPostsByTitleAndContent(pageable, boardId, keyword);
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시판의 게시글 검색(닉네임)
     */
    @GetMapping("/board/{boardId}/search/nickname")
    public ResponseEntity<Page<PostDto>> searchPostsByAuthorNickname(@PathVariable Long boardId,
                                                                     @RequestParam String nickname,
                                                                     Pageable pageable) {
        Page<PostDto> posts = postService.getPostsByAuthorNickname(pageable, boardId, nickname);
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시판의 인기글 검색(제목+내용)
     */
    @GetMapping("/board/{boardId}/popular/search/keyword")
    public ResponseEntity<Page<PostDto>> searchPopularPostsByTitleAndContent(@PathVariable Long boardId,
                                                                             @RequestParam String keyword,
                                                                             Pageable pageable) {
        Page<PostDto> posts = postService.getPopularPostsByTitleAndContent(pageable, boardId, keyword);
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시판의 인기글 검색(닉네임)
     */
    @GetMapping("/board/{boardId}/popular/search/nickname")
    public ResponseEntity<Page<PostDto>> searchPopularPostsByAuthorNickname(@PathVariable Long boardId,
                                                                            @RequestParam String nickname,
                                                                            Pageable pageable) {
        Page<PostDto> posts = postService.getPopularPostsByAuthorNickname(pageable, boardId, nickname);
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시글 작성
     */
    @PostMapping
    public ResponseEntity<PostDto> createPost(@Valid @RequestBody CreatePostRequest request,
                                              @AuthenticationPrincipal CustomUserDetails principal) {
        PostDto saved = postService.create(
                request.boardId(),
                principal.getId(),
                principal.getNickname(),
                request.title(),
                request.content()
        );
        return ResponseEntity.ok(saved);
    }

    /**
     * 게시글 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<PostDto> updatePost(@PathVariable Long id,
                                              @Valid @RequestBody UpdatePostRequest request,
                                              @AuthenticationPrincipal CustomUserDetails principal) {
        PostDto updated = postService.update(id, principal.getId(), request.title(), request.content());
        return ResponseEntity.ok(updated);
    }

    /**
     * 게시글 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        postService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글 삭제
     */
    @DeleteMapping("/board/{boardId}/post/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long boardId,
                                           @PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        postService.deleteForBoardManager(postId, boardId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시판 인기글 목록 조회
     */
    @GetMapping("/board/{boardId}/popular")
    public ResponseEntity<Page<PostDto>> getPopularPosts(@PathVariable Long boardId,
                                                         Pageable pageable) {
        Page<PostDto> posts = postService.getPopularPostsByBoard(boardId, pageable);
        return ResponseEntity.ok(posts);
    }
}