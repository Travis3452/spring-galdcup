package com.example.galdcup.reply;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.reply.dto.CreateReplyRequest;
import com.example.galdcup.reply.dto.ReplyDto;
import com.example.galdcup.reply.dto.UpdateReplyRequest;
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

    /**
     * 대댓글 작성
     */
    @PostMapping
    public ResponseEntity<ReplyDto> createReply(
            @Valid @RequestBody CreateReplyRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ReplyDto reply = replyService.create(
                request.commentId(),
                principal.getId(),
                principal.getNickname(),
                request.content()
        );

        return ResponseEntity.created(URI.create("/api/replies/" + reply.id()))
                .body(reply);
    }

    /**
     * 특정 댓글의 대댓글 조회
     */
    @GetMapping("/comment/{commentId}")
    public ResponseEntity<List<ReplyDto>> getRepliesByComment(@PathVariable Long commentId) {
        List<ReplyDto> replies = replyService.findByComment(commentId);
        return ResponseEntity.ok(replies);
    }

    /**
     * 대댓글 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReplyDto> updateReply(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReplyRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ReplyDto reply = replyService.update(id, principal.getId(), request.content());
        return ResponseEntity.ok(reply);
    }

    /**
     * 대댓글 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ReplyDto> deleteReply(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ReplyDto reply = replyService.delete(id, principal.getId());
        return ResponseEntity.ok(reply);
    }
}