package com.example.galdcup.comment;

import com.example.galdcup.comment.dto.CommentDto;
import com.example.galdcup.comment.dto.CreateCommentRequest;
import com.example.galdcup.comment.dto.UpdateCommentRequest;
import com.example.galdcup.common.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 게시글의 댓글 조회
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<Page<CommentDto>> getCommentsByPost(@PathVariable Long postId,
                                                              Pageable pageable) {
        Page<CommentDto> comments = commentService.findByPost(postId, pageable);
        return ResponseEntity.ok(comments);
    }

    /**
     * 사용자가 작성한 댓글 조회
     */
    @GetMapping("/user/{nickname}")
    public ResponseEntity<Page<CommentDto>> getCommentsByUser(@PathVariable String nickname,
                                                              Pageable pageable) {
        Page<CommentDto> comments = commentService.findByAuthorNickname(nickname, pageable);
        return ResponseEntity.ok(comments);
    }

    /**
     * 댓글 및 대댓글 작성
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/post/{postId}")
    public ResponseEntity<CommentDto> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        CommentDto commentDto = commentService.create(postId, principal.getId(), request);

        return ResponseEntity.created(URI.create("/api/comments/" + commentDto.id()))
                .body(commentDto);
    }

    /**
     * 댓글 및 대댓글 수정
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        CommentDto commentDto = commentService.update(id, principal.getId(), request.content());
        return ResponseEntity.ok(commentDto);
    }

    /**
     * 댓글 및 대댓글 삭제
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<CommentDto> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        CommentDto commentDto = commentService.delete(id, principal.getId());
        return ResponseEntity.ok(commentDto);
    }
}