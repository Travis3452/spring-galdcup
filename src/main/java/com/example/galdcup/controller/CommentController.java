package com.example.galdcup.controller;

import com.example.galdcup.dto.comment.*;
import com.example.galdcup.entity.Comment;
import com.example.galdcup.security.CustomUserDetails;
import com.example.galdcup.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 게시글의 댓글 조회
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentDto>> getCommentsByPost(@PathVariable Long postId) {
        List<CommentDto> comments = commentService.findByPost(postId)
                .stream()
                .map(CommentDto::from)
                .toList();
        return ResponseEntity.ok(comments);
    }

    // 사용자가 작성한 댓글 조회
    @GetMapping("/user/{nickname}")
    public ResponseEntity<List<CommentDto>> getCommentsByUser(@PathVariable String nickname) {
        List<CommentDto> comments = commentService.findByAuthorNickname(nickname)
                .stream()
                .map(CommentDto::from)
                .toList();
        return ResponseEntity.ok(comments);
    }

    // 특정 댓글 조회
    @GetMapping("/{id}")
    public ResponseEntity<CommentDto> getComment(@PathVariable Long id) {
        Optional<Comment> comment = commentService.findById(id);
        return comment.map(c -> ResponseEntity.ok(CommentDto.from(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 댓글 작성
    @PostMapping
    public ResponseEntity<CommentDto> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Comment comment = commentService.create(
                request.postId(),
                principal.getUsername(),
                request.content()
        );

        return ResponseEntity.created(URI.create("/api/comments/" + comment.getId()))
                .body(CommentDto.from(comment));
    }

    // 댓글 수정
    @PutMapping("/{id}")
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Comment comment = commentService.update(id, principal.getUsername(), request.content());
        return ResponseEntity.ok(CommentDto.from(comment));
    }

    // 댓글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        commentService.delete(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}