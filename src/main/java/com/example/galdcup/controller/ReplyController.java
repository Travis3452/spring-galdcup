package com.example.galdcup.controller;

import com.example.galdcup.dto.reply.CreateReplyRequest;
import com.example.galdcup.dto.reply.ReplyDto;
import com.example.galdcup.dto.reply.UpdateReplyRequest;
import com.example.galdcup.entity.Reply;
import com.example.galdcup.security.CustomUserDetails;
import com.example.galdcup.service.ReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/replies")
@RequiredArgsConstructor
public class ReplyController {

    private final ReplyService replyService;

    // 대댓글 작성
    @PostMapping
    public ResponseEntity<ReplyDto> createReply(
            @Valid @RequestBody CreateReplyRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Reply reply = replyService.create(request.commentId(), principal.getUsername(), request.content());
        return ResponseEntity.created(URI.create("/api/replies/" + reply.getId()))
                .body(ReplyDto.from(reply));
    }

    // 특정 댓글의 대댓글 조회
    @GetMapping("/comment/{commentId}")
    public ResponseEntity<List<ReplyDto>> getRepliesByComment(@PathVariable Long commentId) {
        List<ReplyDto> replies = replyService.findByComment(commentId)
                .stream()
                .map(ReplyDto::from)
                .toList();
        return ResponseEntity.ok(replies);
    }

    // 대댓글 수정
    @PutMapping("/{id}")
    public ResponseEntity<ReplyDto> updateReply(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReplyRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Reply reply = replyService.update(id, principal.getUsername(), request.content());
        return ResponseEntity.ok(ReplyDto.from(reply));
    }

    // 대댓글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReply(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        replyService.delete(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}