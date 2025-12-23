package com.example.galdcup.controller;

import com.example.galdcup.dto.post.CreatePostRequest;
import com.example.galdcup.dto.post.PostDto;
import com.example.galdcup.dto.post.UpdatePostRequest;
import com.example.galdcup.entity.Post;
import com.example.galdcup.security.CustomUserDetails;
import com.example.galdcup.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 특정 게시판의 게시글 전체 조회 (페이지네이션 적용)
    @GetMapping("/board/{boardId}")
    public ResponseEntity<Page<PostDto>> getPostsByBoard(@PathVariable Long boardId,
                                                         Pageable pageable) {
        Page<PostDto> posts = postService.findByBoard(boardId, pageable)
                .map(PostDto::from);
        return ResponseEntity.ok(posts);
    }

    // 특정 사용자가 작성한 게시글 조회 (페이지네이션 적용)
    @GetMapping("/user/{nickname}")
    public ResponseEntity<Page<PostDto>> getPostsByUser(@PathVariable String nickname,
                                                        Pageable pageable) {
        Page<PostDto> posts = postService.findByAuthorNickname(nickname, pageable)
                .map(PostDto::from);
        return ResponseEntity.ok(posts);
    }

    // 특정 게시글 조회
    @GetMapping("/{id}")
    public ResponseEntity<PostDto> getPost(@PathVariable Long id) {
        Optional<Post> postOpt = postService.findById(id);
        return postOpt.map(p -> ResponseEntity.ok(PostDto.from(p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 게시글 작성
    @PostMapping
    public ResponseEntity<PostDto> createPost(@Valid @RequestBody CreatePostRequest request,
                                              @AuthenticationPrincipal CustomUserDetails principal) {
        Post saved = postService.create(request.boardId(), principal.getId(), request.title(), request.content());
        return ResponseEntity.created(URI.create("/api/posts/" + saved.getId()))
                .body(PostDto.from(saved));
    }

    // 게시글 수정
    @PutMapping("/{id}")
    public ResponseEntity<PostDto> updatePost(@PathVariable Long id,
                                              @Valid @RequestBody UpdatePostRequest request,
                                              @AuthenticationPrincipal CustomUserDetails principal) {
        Post updated = postService.update(id, principal.getId(), request.title(), request.content());
        return ResponseEntity.ok(PostDto.from(updated));
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        postService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}