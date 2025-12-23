package com.example.galdcup.controller;

import com.example.galdcup.entity.PostReaction;
import com.example.galdcup.security.CustomUserDetails;
import com.example.galdcup.service.PostReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/reactions")
@RequiredArgsConstructor
public class PostReactionController {

    private final PostReactionService PostReactionService;

    /**
     * 게시글에 좋아요/싫어요 반응 추가
     * 요청: { "type": "LIKE" } 또는 { "type": "DISLIKE" }
     */
    @PostMapping
    public ResponseEntity<Void> addReaction(@PathVariable Long postId,
                                            @RequestParam PostReaction.ReactionType type,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        PostReactionService.addReaction(postId, principal.getId(), type);
        return ResponseEntity.ok().build();
    }
}
