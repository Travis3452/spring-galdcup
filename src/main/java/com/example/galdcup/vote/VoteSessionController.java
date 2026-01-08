package com.example.galdcup.vote;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.vote.dto.CreateVoteSessionRequest;
import com.example.galdcup.vote.dto.VoteSessionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boards/{boardId}/vote-session")
@RequiredArgsConstructor
public class VoteSessionController {

    private final VoteSessionService voteSessionService;

    /**
     * 투표 세션 생성
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<VoteSessionDto> createVoteSession(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody CreateVoteSessionRequest request) {

        VoteSession voteSession = voteSessionService.createVoteSession(
                boardId,
                principal.getId(),
                request
        );

        return ResponseEntity.ok(VoteSessionDto.from(voteSession));
    }

    /**
     * 투표 세션 조회
     */
    @GetMapping
    public ResponseEntity<VoteSessionDto> getVoteSession(@PathVariable Long boardId) {
        VoteSession voteSession = voteSessionService.getVoteSession(boardId);
        return ResponseEntity.ok(VoteSessionDto.from(voteSession));
    }
}
