package com.example.galdcup.controller;

import com.example.galdcup.dto.post.*;
import com.example.galdcup.entity.Post;
import com.example.galdcup.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 특정 게시판의 게시글 전체 조회
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<PostDto>> getPostsByBoard(@PathVariable Long boardId) {
        List<PostDto> posts = postService.findByBoard(boardId)
                .stream()
                .map(PostDto::from)
                .toList();
        return ResponseEntity.ok(posts);
    }

    // 특정 사용자가 작성한 게시글 조회
    @GetMapping("/user/{nickname}")
    public ResponseEntity<List<PostDto>> getPostsByUser(@PathVariable String nickname) {
        List<PostDto> posts = postService.findByAuthorNickname(nickname)
                .stream()
                .map(PostDto::from)
                .toList();
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
    public ResponseEntity<PostDto> createPost(@Valid @RequestBody CreatePostRequest request) {
        Post saved = postService.create(request.boardId(), request.authorId(), request.title(), request.content());
        return ResponseEntity.created(URI.create("/api/posts/" + saved.getId()))
                .body(PostDto.from(saved));
    }

    // 게시글 수정
    @PutMapping("/{id}")
    public ResponseEntity<PostDto> updatePost(@PathVariable Long id,
                                              @Valid @RequestBody UpdatePostRequest request) {
        Post updated = postService.update(id, request.title(), request.content());
        return ResponseEntity.ok(PostDto.from(updated));
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.noContent().build();
    }
}